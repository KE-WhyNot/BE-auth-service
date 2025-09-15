package com.youthfi.auth.domain.auth.ui;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.youthfi.auth.domain.auth.application.dto.request.LoginRequest;
import com.youthfi.auth.domain.auth.application.dto.request.SignUpRequest;
import com.youthfi.auth.domain.auth.application.dto.request.TokenReissueRequest;
import com.youthfi.auth.domain.auth.application.dto.response.LoginResponse;
import com.youthfi.auth.domain.auth.application.dto.response.TokenReissueResponse;
import com.youthfi.auth.domain.auth.application.usecase.UserAuthUseCase;
import com.youthfi.auth.global.annotation.CurrentUser;
import com.youthfi.auth.global.common.BaseResponse;
import com.youthfi.auth.global.swagger.AuthApi;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    private final UserAuthUseCase userAuthUseCase;

    @PostMapping("/signup")
    @Override
    public BaseResponse<Void> signup(@Valid @RequestBody SignUpRequest request) {
        userAuthUseCase.signUp(request);
        return BaseResponse.onSuccess();
    }

    @PostMapping("/login")
    @Override
    public BaseResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return BaseResponse.onSuccess(userAuthUseCase.login(request));
    }

    @DeleteMapping("/logout")
    @Override
    public BaseResponse<Void> logout(HttpServletRequest request) {
        userAuthUseCase.logout(request);
        return BaseResponse.onSuccess();
    }

    /**
     * 사용자 ID로 로그아웃 (@CurrentUser 사용)
     */
    @DeleteMapping("/logout/user")
    public BaseResponse<Void> logoutByUserId(@Parameter(hidden = true) @CurrentUser String userId) {
        userAuthUseCase.logout(userId);
        return BaseResponse.onSuccess();
    }

    @PostMapping("/reissue")
    @Override
    public BaseResponse<TokenReissueResponse> reissueToken(@Valid @RequestBody TokenReissueRequest request) {
        return BaseResponse.onSuccess(userAuthUseCase.reissueToken(request));
    }

    /**
     * Nginx Ingress Controller의 auth-request를 위한 토큰 검증 엔드포인트
     * 이 엔드포인트는 내부 네트워크에서만 접근 가능해야 합니다.
     */
    @PostMapping("/verify")
    public BaseResponse<Void> verifyToken(HttpServletRequest request, HttpServletResponse response) {
        String userId = userAuthUseCase.verifyToken(request);
        // 검증된 사용자 ID를 헤더에 추가하여 다운스트림 서비스에서 사용할 수 있도록 함
        response.setHeader("X-User-Id", userId);
        return BaseResponse.onSuccess();
    }
}