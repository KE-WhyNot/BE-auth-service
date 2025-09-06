package com.youthfi.auth.domain.auth.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String refreshTokenPrefix = "REFRESH_TOKEN:";
    private final RedisTemplate<String, String> redisTemplate;

    public void saveRefreshToken(String userId, String refreshToken, Duration timeout) {
        redisTemplate.opsForValue().set(refreshTokenPrefix + userId, refreshToken, timeout);
    }

    public void deleteRefreshToken(String userId) {
        redisTemplate.delete(refreshTokenPrefix + userId);
    }

    public String findByUserId(String userId) {
        return redisTemplate.opsForValue().get(refreshTokenPrefix + userId);
    }
    public boolean isExist(String token, String userId) {
        String savedToken = redisTemplate.opsForValue().get(refreshTokenPrefix + userId);
        boolean exists = savedToken != null && Objects.equals(savedToken, token);

        return exists;
    }
}