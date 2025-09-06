package com.youthfi.auth.domain.auth.ui;

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    private final UserAuthUseCase userAuthUseCase;

    @PostMapping("/signup")
    public BaseResponse<Void> signup(@Valid @RequestBody SignUpRequest request) {
        userAuthUseCase.signUp(request);
        return BaseResponse.onSuccess();
    }

    @PostMapping("/login")
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
}