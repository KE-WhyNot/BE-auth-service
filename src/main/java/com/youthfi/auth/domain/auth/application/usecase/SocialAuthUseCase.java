package com.youthfi.auth.domain.auth.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.youthfi.auth.domain.auth.application.dto.response.LoginResponse;
import com.youthfi.auth.domain.auth.domain.service.SocialOAuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialAuthUseCase {

    private final SocialOAuthService socialOAuthService;

    /**
     * 소셜 로그인: 존재하면 로그인, 없으면 가입 후 로그인.
     */
    @Transactional
    public LoginResponse signInOrSignUp(String providerKey, String code) {
        return socialOAuthService.signInOrSignUp(providerKey, code);
    }

}


