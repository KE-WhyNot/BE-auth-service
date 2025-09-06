package com.youthfi.auth.domain.auth.application.usecase;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import com.youthfi.auth.global.security.TokenProvider;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthUseCase 테스트")
class UserAuthUseCaseTest {

    @Mock
    private UserService userService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private TokenProvider tokenProvider;
    
    @Mock
    private RefreshTokenService refreshTokenService;
    
    @Mock
    private TokenWhitelistService tokenWhitelistService;
    
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    
    @Mock
    private EmailVerificationService emailVerificationService;
    
    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private UserAuthUseCase userAuthUseCase;

    private SignUpRequest validSignUpRequest;
    private LoginRequest validLoginRequest;
    private TokenReissueRequest validTokenReissueRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validSignUpRequest = new SignUpRequest(
                "test@example.com",
                "testuser",
                "password123",
                "홍길동",
                "1990-01-01"
        );

        validLoginRequest = new LoginRequest(
                "testuser",
                "password123"
        );

        validTokenReissueRequest = new TokenReissueRequest(
                "valid.refresh.token"
        );

        testUser = User.builder()
                .userId("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .name("홍길동")
                .birth("1990-01-01")
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signUp_Success() {
        // given
        when(emailVerificationService.isEmailVerified(validSignUpRequest.email())).thenReturn(true);
        when(userService.isAlreadyRegistered(validSignUpRequest.email())).thenReturn(false);
        when(userService.isUserIdAlreadyRegistered(validSignUpRequest.userId())).thenReturn(false);

        // when
        assertDoesNotThrow(() -> userAuthUseCase.signUp(validSignUpRequest));

        // then
        verify(emailVerificationService, times(1)).isEmailVerified(validSignUpRequest.email());
        verify(userService, times(1)).isAlreadyRegistered(validSignUpRequest.email());
        verify(userService, times(1)).isUserIdAlreadyRegistered(validSignUpRequest.userId());
        verify(userService, times(1)).save(validSignUpRequest);
        verify(emailVerificationService, times(1)).removeEmailVerification(validSignUpRequest.email());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 인증되지 않음")
    void signUp_EmailNotVerified_ThrowsException() {
        // given
        when(emailVerificationService.isEmailVerified(validSignUpRequest.email())).thenReturn(false);

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            userAuthUseCase.signUp(validSignUpRequest);
        });

        assertEquals("EMAIL400", exception.getErrorCode().getCode());
        verify(emailVerificationService, times(1)).isEmailVerified(validSignUpRequest.email());
        verify(userService, never()).isAlreadyRegistered(anyString());
        verify(userService, never()).isUserIdAlreadyRegistered(anyString());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 등록된 이메일")
    void signUp_AlreadyRegisteredEmail_ThrowsException() {
        // given
        when(emailVerificationService.isEmailVerified(validSignUpRequest.email())).thenReturn(true);
        when(userService.isAlreadyRegistered(validSignUpRequest.email())).thenReturn(true);

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            userAuthUseCase.signUp(validSignUpRequest);
        });

        assertEquals("AUTH009", exception.getErrorCode().getCode());
        verify(emailVerificationService, times(1)).isEmailVerified(validSignUpRequest.email());
        verify(userService, times(1)).isAlreadyRegistered(validSignUpRequest.email());
        verify(userService, never()).isUserIdAlreadyRegistered(anyString());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 등록된 사용자 ID")
    void signUp_AlreadyRegisteredUserId_ThrowsException() {
        // given
        when(emailVerificationService.isEmailVerified(validSignUpRequest.email())).thenReturn(true);
        when(userService.isAlreadyRegistered(validSignUpRequest.email())).thenReturn(false);
        when(userService.isUserIdAlreadyRegistered(validSignUpRequest.userId())).thenReturn(true);

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            userAuthUseCase.signUp(validSignUpRequest);
        });

        assertEquals("AUTH010", exception.getErrorCode().getCode());
        verify(emailVerificationService, times(1)).isEmailVerified(validSignUpRequest.email());
        verify(userService, times(1)).isAlreadyRegistered(validSignUpRequest.email());
        verify(userService, times(1)).isUserIdAlreadyRegistered(validSignUpRequest.userId());
        verify(userService, never()).save(any());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
        when(userService.findByUserId(validLoginRequest.userId())).thenReturn(testUser);
        when(passwordEncoder.matches(validLoginRequest.password(), testUser.getPassword())).thenReturn(true);
        when(tokenProvider.createAccessToken(testUser.getUserId())).thenReturn("access.token");
        when(tokenProvider.createRefreshToken(testUser.getUserId())).thenReturn("refresh.token");
        when(tokenProvider.getRemainingDuration("refresh.token")).thenReturn(java.util.Optional.of(Duration.ofDays(14)));

        // when
        LoginResponse response = userAuthUseCase.login(validLoginRequest);

        // then
        assertNotNull(response);
        assertEquals("access.token", response.accessToken());
        assertEquals("refresh.token", response.refreshToken());
        verify(userService, times(1)).findByUserId(validLoginRequest.userId());
        verify(passwordEncoder, times(1)).matches(validLoginRequest.password(), testUser.getPassword());
        verify(tokenProvider, times(1)).createAccessToken(testUser.getUserId());
        verify(tokenProvider, times(1)).createRefreshToken(testUser.getUserId());
        verify(refreshTokenService, times(1)).saveRefreshToken(testUser.getUserId(), "refresh.token", Duration.ofDays(14));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_InvalidPassword_ThrowsException() {
        // given
        when(userService.findByUserId(validLoginRequest.userId())).thenReturn(testUser);
        when(passwordEncoder.matches(validLoginRequest.password(), testUser.getPassword())).thenReturn(false);

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            userAuthUseCase.login(validLoginRequest);
        });

        assertEquals("AUTH008", exception.getErrorCode().getCode());
        verify(userService, times(1)).findByUserId(validLoginRequest.userId());
        verify(passwordEncoder, times(1)).matches(validLoginRequest.password(), testUser.getPassword());
        verify(tokenProvider, never()).createAccessToken(anyString());
        verify(tokenProvider, never()).createRefreshToken(anyString());
        verify(refreshTokenService, never()).saveRefreshToken(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("로그아웃 성공 - HttpServletRequest 사용")
    void logout_Success() {
        // given
        when(tokenProvider.getToken(httpServletRequest)).thenReturn(java.util.Optional.of("access.token"));
        when(tokenProvider.getId("access.token")).thenReturn(java.util.Optional.of("testuser"));
        when(tokenProvider.getRemainingDuration("access.token")).thenReturn(java.util.Optional.of(Duration.ofHours(1)));

        // when
        assertDoesNotThrow(() -> userAuthUseCase.logout(httpServletRequest));

        // then
        verify(tokenProvider, times(1)).getToken(httpServletRequest);
        verify(tokenProvider, times(1)).getId("access.token");
        verify(tokenProvider, times(1)).getRemainingDuration("access.token");
        verify(refreshTokenService, times(1)).deleteRefreshToken("testuser");
        verify(tokenWhitelistService, times(1)).deleteWhitelistToken("access.token");
        verify(tokenBlacklistService, times(1)).blacklist("access.token", Duration.ofHours(1));
    }

    @Test
    @DisplayName("로그아웃 실패 - JWT 토큰이 없음")
    void logout_EmptyJwt_ThrowsException() {
        // given
        when(tokenProvider.getToken(httpServletRequest)).thenReturn(java.util.Optional.empty());

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            userAuthUseCase.logout(httpServletRequest);
        });

        assertEquals("AUTH001", exception.getErrorCode().getCode());
        verify(tokenProvider, times(1)).getToken(httpServletRequest);
        verify(tokenProvider, never()).getId(anyString());
        verify(refreshTokenService, never()).deleteRefreshToken(anyString());
    }

    @Test
    @DisplayName("로그아웃 성공 - 사용자 ID 사용")
    void logoutByUserId_Success() {
        // when
        assertDoesNotThrow(() -> userAuthUseCase.logout("testuser"));

        // then
        verify(refreshTokenService, times(1)).deleteRefreshToken("testuser");
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissueToken_Success() {
        // given
        when(tokenProvider.validateToken(validTokenReissueRequest.refreshToken())).thenReturn(true);
        when(tokenProvider.getId(validTokenReissueRequest.refreshToken())).thenReturn(java.util.Optional.of("testuser"));
        when(refreshTokenService.findByUserId("testuser")).thenReturn(validTokenReissueRequest.refreshToken());
        when(tokenProvider.getRemainingDuration(validTokenReissueRequest.refreshToken())).thenReturn(java.util.Optional.of(Duration.ofDays(7)));
        when(tokenProvider.createAccessToken("testuser")).thenReturn("new.access.token");
        when(tokenProvider.createRefreshToken("testuser")).thenReturn("new.refresh.token");
        when(tokenProvider.getRemainingDuration("new.refresh.token")).thenReturn(java.util.Optional.of(Duration.ofDays(14)));

        // when
        TokenReissueResponse response = userAuthUseCase.reissueToken(validTokenReissueRequest);

        // then
        assertNotNull(response);
        assertEquals("new.access.token", response.accessToken());
        assertEquals("new.refresh.token", response.refreshToken());
        verify(tokenProvider, times(1)).validateToken(validTokenReissueRequest.refreshToken());
        verify(tokenProvider, times(1)).getId(validTokenReissueRequest.refreshToken());
        verify(refreshTokenService, times(1)).findByUserId("testuser");
        verify(tokenProvider, times(1)).getRemainingDuration(validTokenReissueRequest.refreshToken());
        verify(tokenProvider, times(1)).createAccessToken("testuser");
        verify(tokenProvider, times(1)).createRefreshToken("testuser");
        verify(refreshTokenService, times(1)).deleteRefreshToken("testuser");
        verify(refreshTokenService, times(1)).saveRefreshToken("testuser", "new.refresh.token", Duration.ofDays(14));
        verify(tokenBlacklistService, times(1)).blacklist(validTokenReissueRequest.refreshToken(), Duration.ofDays(7));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 리프레시 토큰")
    void reissueToken_InvalidRefreshToken_ThrowsException() {
        // given
        when(tokenProvider.validateToken(validTokenReissueRequest.refreshToken())).thenReturn(false);

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            userAuthUseCase.reissueToken(validTokenReissueRequest);
        });

        assertEquals("AUTH007", exception.getErrorCode().getCode());
        verify(tokenProvider, times(1)).validateToken(validTokenReissueRequest.refreshToken());
        verify(tokenProvider, never()).getId(anyString());
        verify(refreshTokenService, never()).findByUserId(anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 저장된 리프레시 토큰과 불일치")
    void reissueToken_MismatchedRefreshToken_ThrowsException() {
        // given
        when(tokenProvider.validateToken(validTokenReissueRequest.refreshToken())).thenReturn(true);
        when(tokenProvider.getId(validTokenReissueRequest.refreshToken())).thenReturn(java.util.Optional.of("testuser"));
        when(refreshTokenService.findByUserId("testuser")).thenReturn("different.refresh.token");

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            userAuthUseCase.reissueToken(validTokenReissueRequest);
        });

        assertEquals("AUTH007", exception.getErrorCode().getCode());
        verify(tokenProvider, times(1)).validateToken(validTokenReissueRequest.refreshToken());
        verify(tokenProvider, times(1)).getId(validTokenReissueRequest.refreshToken());
        verify(refreshTokenService, times(1)).findByUserId("testuser");
        verify(tokenProvider, never()).createAccessToken(anyString());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 리프레시 토큰이 저장되지 않음")
    void reissueToken_NoStoredRefreshToken_ThrowsException() {
        // given
        when(tokenProvider.validateToken(validTokenReissueRequest.refreshToken())).thenReturn(true);
        when(tokenProvider.getId(validTokenReissueRequest.refreshToken())).thenReturn(java.util.Optional.of("testuser"));
        when(refreshTokenService.findByUserId("testuser")).thenReturn(null);

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            userAuthUseCase.reissueToken(validTokenReissueRequest);
        });

        assertEquals("AUTH007", exception.getErrorCode().getCode());
        verify(tokenProvider, times(1)).validateToken(validTokenReissueRequest.refreshToken());
        verify(tokenProvider, times(1)).getId(validTokenReissueRequest.refreshToken());
        verify(refreshTokenService, times(1)).findByUserId("testuser");
        verify(tokenProvider, never()).createAccessToken(anyString());
    }
}
