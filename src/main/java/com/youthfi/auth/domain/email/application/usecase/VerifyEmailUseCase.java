package com.youthfi.auth.domain.email.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.youthfi.auth.domain.email.application.dto.request.VerifyEmailRequest;
import com.youthfi.auth.domain.email.application.dto.response.EmailVerificationResponse;
import com.youthfi.auth.domain.email.domain.service.EmailService;
import com.youthfi.auth.domain.email.domain.service.EmailVerificationService;
import com.youthfi.auth.global.exception.RestApiException;
import static com.youthfi.auth.global.exception.code.status.EmailErrorStatus.EMAIL_INVALID_TOKEN;
import static com.youthfi.auth.global.exception.code.status.EmailErrorStatus.EMAIL_VERIFICATION_FAILED;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VerifyEmailUseCase {

    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;

    // 인증 토큰 TTL (30분)
    private static final long VERIFICATION_TTL_SECONDS = 1800;

    public EmailVerificationResponse verifyEmail(VerifyEmailRequest request) {
        String token = request.verifyToken();
        
        log.info("이메일 인증 검증 요청");
        
        try {
            // JWT 토큰 검증
            emailService.verifySignupToken(token);
            
            // 토큰에서 이메일 추출 (토큰 검증 후에 이메일을 추출)
            String email = extractEmailFromToken(token);
            if (email == null) {
                throw new RestApiException(EMAIL_INVALID_TOKEN);
            }
            
            // 이메일 인증 상태를 Redis에 저장 (30분 TTL)
            emailVerificationService.markEmailAsVerified(email, VERIFICATION_TTL_SECONDS);
            
            log.info("이메일 인증 완료: {}", email);
            
            return new EmailVerificationResponse(true, VERIFICATION_TTL_SECONDS);
            
        } catch (IllegalArgumentException e) {
            log.warn("이메일 인증 실패: {}", e.getMessage());
            throw new RestApiException(EMAIL_INVALID_TOKEN);
        } catch (RestApiException e) {
            // 이미 RestApiException인 경우 그대로 던지기
            throw e;
        } catch (Exception e) {
            log.error("이메일 인증 검증 중 오류: {}", e);
            throw new RestApiException(EMAIL_VERIFICATION_FAILED);
        }
    }
    
    private String extractEmailFromToken(String token) {
        try {
            // TokenProvider를 통해 이메일 추출
            return emailService.getEmailFromToken(token);
        } catch (Exception e) {
            log.warn("토큰에서 이메일 추출 실패: {}", e.getMessage());
            throw new RestApiException(EMAIL_INVALID_TOKEN);
        }
    }
}
