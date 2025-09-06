package com.youthfi.auth.domain.email.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "인증 토큰은 필수입니다.")
        String verifyToken
) {}
