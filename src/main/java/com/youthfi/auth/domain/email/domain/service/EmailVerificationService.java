package com.youthfi.auth.domain.email.domain.service;

import java.time.Duration;
import java.util.Random;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final static String VERIFICATION_PREFIX = "EMAIL_VERIFIED:";
    private final static String VERIFICATION_CODE_PREFIX = "EMAIL_VERIFICATION_CODE:";
    private final static String VERIFICATION_ATTEMPT_PREFIX = "EMAIL_VERIFICATION_ATTEMPT:";
    
    // 인증 코드 TTL (5분)
    private static final long VERIFICATION_CODE_TTL_SECONDS = 300;
    // 최대 시도 횟수 (5회)
    private static final int MAX_ATTEMPT_COUNT = 5;
    // 시도 횟수 TTL (10분)
    private static final long ATTEMPT_TTL_SECONDS = 600;

    /**
     * 이메일 인증 상태를 Redis에 저장
     * @param email 인증된 이메일
     * @param ttlSeconds TTL (초)
     */
    public void markEmailAsVerified(String email, long ttlSeconds) {
        try {
            String key = VERIFICATION_PREFIX + email;
            redisTemplate.opsForValue().set(key, "true", Duration.ofSeconds(ttlSeconds));
            log.info("이메일 인증 상태 저장: {}, TTL: {}초", email, ttlSeconds);
        } catch (Exception e) {
            log.error("이메일 인증 상태 저장 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 인증 상태 저장에 실패했습니다.", e);
        }
    }

    /**
     * 이메일 인증 상태 확인
     * @param email 확인할 이메일
     * @return 인증 완료 여부
     */
    public boolean isEmailVerified(String email) {
        try {
            String key = VERIFICATION_PREFIX + email;
            String verified = redisTemplate.opsForValue().get(key);
            boolean result = "true".equals(verified);
            log.debug("이메일 인증 상태 확인: {}, 결과: {}", email, result);
            return result;
        } catch (Exception e) {
            log.warn("이메일 인증 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 6자리 랜덤 인증 코드 생성
     * @return 6자리 인증 코드
     */
    public String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(code);
    }

    /**
     * 이메일 인증 코드 저장
     * @param email 이메일 주소
     * @param code 인증 코드
     */
    public void saveVerificationCode(String email, String code) {
        try {
            String key = VERIFICATION_CODE_PREFIX + email;
            redisTemplate.opsForValue().set(key, code, Duration.ofSeconds(VERIFICATION_CODE_TTL_SECONDS));
            log.info("이메일 인증 코드 저장: {}, TTL: {}초", email, VERIFICATION_CODE_TTL_SECONDS);
        } catch (Exception e) {
            log.error("이메일 인증 코드 저장 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 인증 코드 저장에 실패했습니다.", e);
        }
    }

    /**
     * 이메일 인증 코드 검증
     * @param email 이메일 주소
     * @param inputCode 입력받은 인증 코드
     * @return 검증 성공 여부
     */
    public boolean verifyCode(String email, String inputCode) {
        try {
            // 시도 횟수 확인
            if (isMaxAttemptsExceeded(email)) {
                log.warn("이메일 인증 시도 횟수 초과: {}", email);
                throw new RuntimeException("인증 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
            }

            String key = VERIFICATION_CODE_PREFIX + email;
            String storedCode = redisTemplate.opsForValue().get(key);
            
            if (storedCode == null) {
                log.warn("이메일 인증 코드가 존재하지 않음: {}", email);
                incrementAttemptCount(email);
                return false;
            }

            boolean isValid = storedCode.equals(inputCode);
            
            if (isValid) {
                // 인증 성공 시 코드 삭제
                redisTemplate.delete(key);
                // 시도 횟수도 삭제
                String attemptKey = VERIFICATION_ATTEMPT_PREFIX + email;
                redisTemplate.delete(attemptKey);
                log.info("이메일 인증 코드 검증 성공: {}", email);
            } else {
                log.warn("이메일 인증 코드 불일치: {}", email);
                incrementAttemptCount(email);
            }
            
            return isValid;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("이메일 인증 코드 검증 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 인증 코드 검증에 실패했습니다.", e);
        }
    }

    /**
     * 이메일 인증 코드 삭제
     * @param email 이메일 주소
     */
    public void removeVerificationCode(String email) {
        try {
            String key = VERIFICATION_CODE_PREFIX + email;
            redisTemplate.delete(key);
            log.info("이메일 인증 코드 삭제: {}", email);
        } catch (Exception e) {
            log.error("이메일 인증 코드 삭제 실패: {}", e.getMessage());
        }
    }

    /**
     * 시도 횟수 증가
     * @param email 이메일 주소
     */
    private void incrementAttemptCount(String email) {
        try {
            String key = VERIFICATION_ATTEMPT_PREFIX + email;
            Long count = redisTemplate.opsForValue().increment(key);
            
            if (count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(ATTEMPT_TTL_SECONDS));
            }
            
            log.info("이메일 인증 시도 횟수 증가: {}, {}회", email, count);
        } catch (Exception e) {
            log.error("이메일 인증 시도 횟수 증가 실패: {}", e.getMessage());
        }
    }

    /**
     * 최대 시도 횟수 초과 여부 확인
     * @param email 이메일 주소
     * @return 최대 시도 횟수 초과 여부
     */
    private boolean isMaxAttemptsExceeded(String email) {
        try {
            String key = VERIFICATION_ATTEMPT_PREFIX + email;
            String countStr = redisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;
            return count >= MAX_ATTEMPT_COUNT;
        } catch (Exception e) {
            log.warn("이메일 인증 시도 횟수 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 현재 시도 횟수 조회
     * @param email 이메일 주소
     * @return 현재 시도 횟수
     */
    public int getCurrentAttemptCount(String email) {
        try {
            String key = VERIFICATION_ATTEMPT_PREFIX + email;
            String countStr = redisTemplate.opsForValue().get(key);
            return countStr != null ? Integer.parseInt(countStr) : 0;
        } catch (Exception e) {
            log.warn("이메일 인증 시도 횟수 조회 실패: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 남은 시도 횟수 조회
     * @param email 이메일 주소
     * @return 남은 시도 횟수
     */
    public int getRemainingAttemptCount(String email) {
        int currentCount = getCurrentAttemptCount(email);
        return Math.max(0, MAX_ATTEMPT_COUNT - currentCount);
    }

    /**
     * 이메일 인증 상태 제거
     * @param email 제거할 이메일
     */
    public void removeEmailVerification(String email) {
        try {
            String key = VERIFICATION_PREFIX + email;
            redisTemplate.delete(key);
            log.info("이메일 인증 상태 제거: {}", email);
        } catch (Exception e) {
            log.error("이메일 인증 상태 제거 실패: {}", e.getMessage());
        }
    }
}
