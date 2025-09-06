package com.youthfi.auth.domain.auth.application.usecase;

import java.time.Duration;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.youthfi.auth.domain.auth.application.dto.request.LoginRequest;
import com.youthfi.auth.domain.auth.application.dto.request.SignUpRequest;
import com.youthfi.auth.domain.auth.application.dto.request.TokenReissueRequest;
import com.youthfi.auth.domain.auth.application.dto.response.LoginResponse;
import com.youthfi.auth.domain.auth.application.dto.response.TokenReissueResponse;
import com.youthfi.auth.domain.auth.domain.entity.User;
import com.youthfi.auth.domain.auth.domain.service.RefreshTokenService;
import com.youthfi.auth.domain.auth.domain.service.TokenBlacklistService;
import com.youthfi.auth.domain.auth.domain.service.TokenWhitelistService;
import com.youthfi.auth.domain.auth.domain.service.UserService;
import com.youthfi.auth.domain.email.domain.service.EmailVerificationService;
import com.youthfi.auth.global.exception.RestApiException;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.ALREADY_REGISTERED_EMAIL;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.ALREADY_REGISTERED_USER_ID;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.EMPTY_JWT;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.EXPIRED_REFRESH_TOKEN;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.INVALID_ACCESS_TOKEN;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.INVALID_REFRESH_TOKEN;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.LOGIN_ERROR;
import static com.youthfi.auth.global.exception.code.status.EmailErrorStatus.EMAIL_NOT_VERIFIED;
import com.youthfi.auth.global.security.TokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserAuthUseCase {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenWhitelistService tokenWhitelistService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailVerificationService emailVerificationService;

    public void signUp(SignUpRequest request) {
        // 이메일 인증 상태 확인
        if (!emailVerificationService.isEmailVerified(request.email())) {
            throw new RestApiException(EMAIL_NOT_VERIFIED);
        }
        
        if (userService.isAlreadyRegistered(request.email())) {
            throw new RestApiException(ALREADY_REGISTERED_EMAIL);
        }
        if (userService.isUserIdAlreadyRegistered(request.userId())) {
            throw new RestApiException(ALREADY_REGISTERED_USER_ID);
        }
        
        // 회원가입 처리
        userService.save(request);
        
        // 이메일 인증 상태 제거 (한 번만 사용 가능)
        emailVerificationService.removeEmailVerification(request.email());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userService.findByUserId(request.userId());
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RestApiException(LOGIN_ERROR);
        }
        String access = tokenProvider.createAccessToken(user.getUserId());
        String refresh = tokenProvider.createRefreshToken(user.getUserId());
        Duration ttl = tokenProvider.getRemainingDuration(refresh).orElse(Duration.ofDays(14));
        refreshTokenService.saveRefreshToken(user.getUserId(), refresh, ttl);
        return new LoginResponse(access, refresh);
    }

    public void logout(HttpServletRequest request) {
        String accessToken = tokenProvider.getToken(request)
                .orElseThrow(() -> new RestApiException(EMPTY_JWT));
        String userId = tokenProvider.getId(accessToken)
                .orElseThrow(() -> new RestApiException(INVALID_ACCESS_TOKEN));
        Duration expiration = tokenProvider.getRemainingDuration(accessToken)
                .orElseThrow(() -> new RestApiException(INVALID_ACCESS_TOKEN));
        refreshTokenService.deleteRefreshToken(userId);
        tokenWhitelistService.deleteWhitelistToken(accessToken);
        tokenBlacklistService.blacklist(accessToken, expiration);
    }

    /**
     * 사용자 ID로 로그아웃 처리
     * @CurrentUser 어노테이션과 함께 사용하기 위한 메서드
     */
    public void logout(String userId) {
        // 사용자의 리프레시 토큰만 삭제 (액세스 토큰은 만료 시까지 유효)
        refreshTokenService.deleteRefreshToken(userId);
    }

    public TokenReissueResponse reissueToken(TokenReissueRequest request) {
        String refreshToken = request.refreshToken();

        // 1. refresh token 유효성 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RestApiException(INVALID_REFRESH_TOKEN);
        }

        // 2. refresh token에서 userId 추출
        String userId = tokenProvider.getId(refreshToken)
                .orElseThrow(() -> new RestApiException(INVALID_REFRESH_TOKEN));

        // 3. Redis에 저장된 refresh token과 일치하는지 확인
        String savedRefreshToken = refreshTokenService.findByUserId(userId);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new RestApiException(INVALID_REFRESH_TOKEN);
        }

        // 4. refresh token 만료 시간 확인
        Duration remainingTime = tokenProvider.getRemainingDuration(refreshToken)
                .orElseThrow(() -> new RestApiException(EXPIRED_REFRESH_TOKEN));

        // 5. 새로운 access token 발급
        String newAccessToken = tokenProvider.createAccessToken(userId);

        // 6. 새로운 refresh token 발급 (기존 것 교체)
        String newRefreshToken = tokenProvider.createRefreshToken(userId);
        Duration newTtl = tokenProvider.getRemainingDuration(newRefreshToken).orElse(Duration.ofDays(14));

        // 7. 기존 refresh token 삭제하고 새로운 것 저장
        refreshTokenService.deleteRefreshToken(userId);
        refreshTokenService.saveRefreshToken(userId, newRefreshToken, newTtl);

        // 8. 기존 refresh token을 블랙리스트에 추가 (보안 강화)
        tokenBlacklistService.blacklist(refreshToken, remainingTime);

        return new TokenReissueResponse(
                newAccessToken,
                newRefreshToken
        );
    }
}