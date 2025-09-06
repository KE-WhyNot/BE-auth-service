package com.youthfi.auth.domain.email.domain.service;

import java.time.Duration;

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
