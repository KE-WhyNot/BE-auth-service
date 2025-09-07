package com.youthfi.auth.global.security;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPatternParser;

import com.youthfi.auth.domain.auth.domain.service.RefreshTokenService;
import com.youthfi.auth.domain.auth.domain.service.TokenWhitelistService;
import com.youthfi.auth.global.exception.RestApiException;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.EMPTY_JWT;
import static com.youthfi.auth.global.exception.code.status.AuthErrorStatus.INVALID_ACCESS_TOKEN;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final ExcludeAuthPathProperties excludeAuthPathProperties;
    private final RefreshTokenService refreshTokenService;
    private final TokenWhitelistService tokenWhitelistService;

    private final PathPatternParser pathPatternParser = new PathPatternParser();

    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.debug("[JwtAuthFilter] start: {} {}", request.getMethod(), request.getRequestURI());
        try {
            if (isExcludedPath(request)) {
                log.debug("[JwtAuthFilter] excluded path, skip auth");
                filterChain.doFilter(request, response);
                return;
            }

            String token = tokenProvider.getToken(request)
                    .orElseThrow(() -> {
                        log.warn("[JwtAuthFilter] missing Authorization header");
                        return new RestApiException(EMPTY_JWT);
                    });

            // 토큰 캐시 확인
            if (tokenWhitelistService.isWhitelistToken(token)) {
                log.debug("[JwtAuthFilter] token whitelisted");
                setAuthentication(token);
                filterChain.doFilter(request, response);
                return;
            }

            // 토큰 검증
            if (tokenProvider.validateToken(token)) {
                log.info("[JwtAuthFilter] token valid, authenticating user");
                setAuthentication(token);
                // 토큰 캐시
                tokenWhitelistService.whitelist(token, Duration.ofSeconds(30));
            } else {
                log.warn("[JwtAuthFilter] invalid token");
                throw new RestApiException(INVALID_ACCESS_TOKEN);
            }

            filterChain.doFilter(request, response);
        } catch (RestApiException e) {
            log.error("[JwtAuthFilter] authentication error: {}", e.getErrorCode().getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            String message = e.getErrorCode().getMessage() != null ? e.getErrorCode().getMessage() : "인증 오류가 발생했습니다.";
            String jsonResponse = String.format("{\"message\": \"%s\"}", message);

            PrintWriter writer = response.getWriter();
            writer.write(jsonResponse);
            writer.flush();
            writer.close();
        }
    }

    public boolean isExcludedPath(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod());

        return excludeAuthPathProperties.getPaths().stream()
                .anyMatch(authPath ->
                        pathPatternParser.parse(authPath.getPathPattern())
                                .matches(PathContainer.parsePath(requestPath))
                                && requestMethod.equals(HttpMethod.valueOf(authPath.getMethod()))
                );
    }

    private void setAuthentication(String token) {
        if (tokenProvider.validateToken(token)) {
            Authentication authentication = tokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}