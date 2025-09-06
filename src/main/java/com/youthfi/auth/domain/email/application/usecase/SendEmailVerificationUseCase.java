package com.youthfi.auth.domain.email.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.youthfi.auth.domain.email.application.dto.request.SendVerificationRequest;
import com.youthfi.auth.domain.email.domain.service.EmailService;
import com.youthfi.auth.global.exception.RestApiException;
import static com.youthfi.auth.global.exception.code.status.EmailErrorStatus.EMAIL_COOLDOWN_ACTIVE;
import static com.youthfi.auth.global.exception.code.status.EmailErrorStatus.EMAIL_DAILY_LIMIT_EXCEEDED;
import static com.youthfi.auth.global.exception.code.status.EmailErrorStatus.EMAIL_SEND_FAILED;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SendEmailVerificationUseCase {

    private final EmailService emailService;

    public void sendVerification(SendVerificationRequest request) {
        String email = request.email();
        
        log.info("이메일 인증 발송 요청: {}", email);
        
        try {
            // 이메일 발송 (Rate Limiting 포함)
            emailService.sendVerificationLink(email);
            log.info("이메일 인증 발송 완료: {}", email);
        } catch (RuntimeException e) {
            log.error("이메일 인증 발송 실패: {} - {}", email, e.getMessage());
            
            // 에러 메시지에 따라 적절한 예외 처리
            if (e.getMessage().contains("쿨다운")) {
                throw new RestApiException(EMAIL_COOLDOWN_ACTIVE);
            } else if (e.getMessage().contains("횟수")) {
                throw new RestApiException(EMAIL_DAILY_LIMIT_EXCEEDED);
            } else {
                throw new RestApiException(EMAIL_SEND_FAILED);
            }
        } catch (Exception e) {
            log.error("이메일 인증 발송 실패: {}", email, e);
            throw new RestApiException(EMAIL_SEND_FAILED);
        }
    }
}
