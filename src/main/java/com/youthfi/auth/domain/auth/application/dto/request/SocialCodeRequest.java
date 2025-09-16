package com.youthfi.auth.domain.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SocialCodeRequest(
        @NotBlank String code
) {}


