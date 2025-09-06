package com.youthfi.auth.global.swagger;

import com.youthfi.auth.domain.email.application.dto.request.SendVerificationRequest;
import com.youthfi.auth.domain.email.application.dto.request.VerifyEmailRequest;
import com.youthfi.auth.domain.email.application.dto.response.EmailVerificationResponse;
import com.youthfi.auth.global.common.BaseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 이메일 인증 관련 API 인터페이스
 */
@Tag(name = "이메일 인증", description = "이메일 인증 발송 및 검증 API")
public interface EmailApi extends BaseApi {

    @Operation(
            summary = "이메일 인증 링크 발송",
            description = "회원가입용 이메일 인증 링크를 발송합니다. 쿨다운 및 일일 발송 제한이 적용됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "인증 링크 발송 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 이메일 형식",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "쿨다운 중이거나 일일 발송 횟수 초과",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "이메일 발송 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            )
    })
    BaseResponse<Void> sendVerification(SendVerificationRequest request);

    @Operation(
            summary = "이메일 인증 검증",
            description = "JWT 토큰을 사용하여 이메일 인증을 검증합니다. 토큰에서 이메일을 자동으로 추출하여 Redis에 인증 상태를 저장합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "이메일 인증 성공",
                    content = @Content(schema = @Schema(implementation = EmailVerificationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 토큰 또는 인증 실패",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            )
    })
    BaseResponse<EmailVerificationResponse> verifyEmail(VerifyEmailRequest request);
}
