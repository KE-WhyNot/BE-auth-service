package com.youthfi.auth.domain.auth.ui;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.youthfi.auth.domain.auth.application.dto.request.LoginRequest;
import com.youthfi.auth.domain.auth.application.dto.request.SignUpRequest;
import com.youthfi.auth.domain.auth.application.dto.request.SocialCodeRequest;
import com.youthfi.auth.domain.auth.application.dto.request.TokenReissueRequest;
import com.youthfi.auth.domain.auth.application.dto.response.LoginResponse;
import com.youthfi.auth.domain.auth.application.dto.response.TokenReissueResponse;
import com.youthfi.auth.domain.auth.application.usecase.SocialAuthUseCase;
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
    private final SocialAuthUseCase socialAuthUseCase;

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
     * Nginx Ingress Controller의 auth-request를 위한 토큰 검증 엔드포인트 (사용할지 사용 안할지 모르겠음)
     */
    @PostMapping("/verify")
    public BaseResponse<Void> verifyToken(HttpServletRequest request, HttpServletResponse response) {
        userAuthUseCase.verifyToken(request);
        return BaseResponse.onSuccess();
    }

    /** provider path variable 버전 */
    @PostMapping("/login/{provider}")
    public BaseResponse<LoginResponse> socialLoginByPath(@PathVariable String provider,
                                                         @Valid @RequestBody SocialCodeRequest request) {
        LoginResponse response = socialAuthUseCase.signInOrSignUp(provider, request.code());
        return BaseResponse.onSuccess(response);
    }
}