package com.youthfi.auth.global.interceptor;

import com.youthfi.auth.domain.auth.domain.service.TokenBlacklistService;
import com.youthfi.auth.global.exception.RestApiException;
import com.youthfi.auth.global.security.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.EMPTY_JWT;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.EXPIRED_MEMBER_JWT;

@Component
@RequiredArgsConstructor
public class JwtBlacklistInterceptor implements HandlerInterceptor {

    private final TokenProvider tokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String token = tokenProvider.getToken(req)
                .orElseThrow(() -> new RestApiException(EMPTY_JWT));

        boolean isBlack = tokenBlacklistService.isBlacklistToken(token);
        if (isBlack) {
            throw new RestApiException(EXPIRED_MEMBER_JWT);
        }
        return true;
    }
}