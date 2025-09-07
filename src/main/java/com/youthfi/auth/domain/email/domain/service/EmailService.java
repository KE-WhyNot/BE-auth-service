package com.youthfi.auth.domain.email.domain.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.youthfi.auth.global.security.TokenProvider;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final TokenProvider tokenProvider;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${app.client.url}")
    private String clientUrl;

    // Redis 키 접두사
    private final static String COOLDOWN_PREFIX = "EMAIL_COOLDOWN:";
    private final static String ATTEMPT_PREFIX = "EMAIL_ATTEMPT:";
    private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Rate Limiting 상수
    private static final int MAX_DAILY_ATTEMPTS = 5;
    private static final long COOLDOWN_SECONDS = 60;

    // JWT 토큰 생성 (기존 TokenProvider 사용)
    private String createToken(String email, String type) {
        return tokenProvider.createEmailVerificationToken(email, type);
    }

    /**
     * 회원가입용 이메일 인증 링크 발송
     * @param email 인증할 이메일 주소
     */
    public void sendVerificationLink(String email) {
        // 쿨다운 확인
        if (isInCooldown(email)) {
            log.warn("이메일 발송 쿨다운 중: {}", email);
            throw new RuntimeException("이메일 발송 쿨다운 중입니다. 잠시 후 다시 시도해주세요.");
        }
        
        // 일일 발송 시도 횟수 확인
        if (isDailyLimitExceeded(email)) {
            log.warn("일일 이메일 발송 시도 횟수 초과: {}", email);
            throw new RuntimeException("일일 이메일 발송 횟수를 초과했습니다.");
        }

        String token = createToken(email, "signup");
        String link = clientUrl + "/signup?verifyToken=" + token;

        String subject = "[YouthFi] 회원가입 이메일 인증";
        String html = ""
                + "<div style=\"font-family:Arial,sans-serif;color:#333;padding:20px;max-width:600px;margin:auto;\">"
                + "  <div style=\"text-align:center;margin-bottom:20px;\">"
                + "    <h1 style=\"margin:0;font-size:24px;color:#0064FF;\">YouthFi</h1>"
                + "  </div>"
                + "  <p style=\"font-size:16px;\">안녕하세요!</p>"
                + "  <p style=\"font-size:16px;\">회원가입 인증을 위해 아래 버튼을 클릭해주세요.</p>"
                + "  <div style=\"background:#f5f5f5;padding:15px;text-align:center;margin:20px 0;\">"
                + "    <a href=\"" + link + "\" style=\"display:inline-block;padding:12px 24px;background:#0064FF;color:#fff;text-decoration:none;border-radius:4px;\">"
                + "      이메일 인증하기"
                + "    </a>"
                + "  </div>"
                + "  <p style=\"font-size:14px;color:#888;\">이 링크는 15분 후 만료됩니다.</p>"
                + "  <p style=\"font-size:14px;\">요청하지 않으셨다면 고객지원으로 문의해주세요.</p>"
                + "  <hr style=\"border:none;border-top:1px solid #eee;margin:30px 0;\"/>"
                + "  <div style=\"font-size:12px;color:#aaa;text-align:center;\">YouthFi Inc, Seoul, Korea</div>"
                + "</div>";

        sendHtmlMail(email, subject, html);
        
        // 쿨다운 설정
        setCooldown(email, COOLDOWN_SECONDS);
        
        // 발송 시도 횟수 증가
        incrementAttemptCount(email);
    }

    /**
     * 회원가입용 JWT 토큰 검증
     * @param token 검증할 JWT 토큰
     * @return 검증 성공 여부
     */
    public boolean verifySignupToken(String token) {
        try {
            boolean isValid = tokenProvider.validateEmailVerificationToken(token, "signup");
            if (!isValid) {
                throw new IllegalArgumentException("유효하지 않은 인증 토큰입니다.");
            }
            return true;
        } catch (Exception ex) {
            throw new IllegalArgumentException("인증 링크가 만료되었거나 유효하지 않습니다.");
        }
    }

    /**
     * 비밀번호 재설정용 이메일 링크 발송
     * @param email 인증할 이메일 주소
     */
    public void sendPasswordResetLink(String email) {
        String token = createToken(email, "reset");
        String link = clientUrl + "/password-change?verifyToken=" + token;

        String subject = "[YouthFi] 비밀번호 재설정 이메일";
        String html = ""
                + "<div style=\"font-family:Arial,sans-serif;color:#333;padding:20px;max-width:600px;margin:auto;\">"
                + "  <div style=\"text-align:center;margin-bottom:20px;\">"
                + "    <h1 style=\"margin:0;font-size:24px;color:#E74C3C;\">YouthFi</h1>"
                + "  </div>"
                + "  <p style=\"font-size:16px;\">비밀번호 재설정을 요청하셨습니다.</p>"
                + "  <p style=\"font-size:16px;\">아래 버튼을 클릭하여 비밀번호를 변경해주세요.</p>"
                + "  <div style=\"background:#f5f5f5;padding:15px;text-align:center;margin:20px 0;\">"
                + "    <a href=\"" + link + "\" style=\"display:inline-block;padding:12px 24px;background:#E74C3C;color:#fff;text-decoration:none;border-radius:4px;\">"
                + "      비밀번호 재설정하기"
                + "    </a>"
                + "  </div>"
                + "  <p style=\"font-size:14px;color:#888;\">이 링크는 15분 후 만료됩니다.</p>"
                + "  <p style=\"font-size:14px;\">요청하지 않으셨다면 고객지원으로 문의해주세요.</p>"
                + "  <hr style=\"border:none;border-top:1px solid #eee;margin:30px 0;\"/>"
                + "  <div style=\"font-size:12px;color:#aaa;text-align:center;\">YouthFi Inc, Seoul, Korea</div>"
                + "</div>";

        sendHtmlMail(email, subject, html);
    }

    /**
     * 비밀번호 재설정용 JWT 토큰 검증
     * @param token 검증할 JWT 토큰
     * @return 검증 성공 여부
     */
    public boolean verifyPasswordResetToken(String token) {
        return tokenProvider.validateEmailVerificationToken(token, "reset");
    }

    /**
     * JWT 토큰에서 이메일 추출
     * @param token JWT 토큰
     * @return 이메일 주소
     */
    public String getEmailFromToken(String token) {
        return tokenProvider.getEmailFromVerificationToken(token).orElse(null);
    }

    // 공통 HTML 메일 전송
    private void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setFrom(fromEmail, "YouthFi");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("이메일 전송 성공: {}", to);
        } catch (MessagingException ex) {
            log.error("HTML 메일 전송 실패 to={}: {}", to, ex.getMessage(), ex);
            throw new RuntimeException("이메일 전송에 실패했습니다.", ex);
        } catch (Exception ex) {
            log.error("HTML 메일 전송 실패 to={}: {}", to, ex.getMessage(), ex);
            throw new RuntimeException("이메일 전송에 실패했습니다.", ex);
        }
    }

    // Rate Limiting 관련 메서드들
    private boolean isInCooldown(String email) {
        try {
            String key = COOLDOWN_PREFIX + email;
            String cooldown = redisTemplate.opsForValue().get(key);
            return cooldown != null;
        } catch (Exception e) {
            log.warn("이메일 쿨다운 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    private void setCooldown(String email, long cooldownSeconds) {
        try {
            String key = COOLDOWN_PREFIX + email;
            redisTemplate.opsForValue().set(key, "true", Duration.ofSeconds(cooldownSeconds));
            log.info("이메일 쿨다운 설정: {}, {}초", email, cooldownSeconds);
        } catch (Exception e) {
            log.error("이메일 쿨다운 설정 실패: {}", e.getMessage());
        }
    }

    private int getTodayAttemptCount(String email) {
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);
            String key = ATTEMPT_PREFIX + email + ":" + today;
            String countStr = redisTemplate.opsForValue().get(key);
            return countStr != null ? Integer.parseInt(countStr) : 0;
        } catch (Exception e) {
            log.warn("이메일 발송 시도 횟수 확인 실패: {}", e.getMessage());
            return 0;
        }
    }

    private void incrementAttemptCount(String email) {
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);
            String key = ATTEMPT_PREFIX + email + ":" + today;
            
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == 1) {
                long secondsUntilMidnight = getSecondsUntilMidnight();
                redisTemplate.expire(key, Duration.ofSeconds(secondsUntilMidnight));
            }
            
            log.info("이메일 발송 시도 횟수 증가: {}, {}회", email, count);
        } catch (Exception e) {
            log.error("이메일 발송 시도 횟수 증가 실패: {}", e.getMessage());
        }
    }

    private boolean isDailyLimitExceeded(String email) {
        return getTodayAttemptCount(email) >= MAX_DAILY_ATTEMPTS;
    }

    private long getSecondsUntilMidnight() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return java.time.Duration.between(
                java.time.LocalDateTime.now(),
                tomorrow.atStartOfDay()
        ).getSeconds();
    }
}
