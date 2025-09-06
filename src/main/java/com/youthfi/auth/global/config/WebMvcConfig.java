package com.youthfi.auth.global.config;

import com.youthfi.auth.global.interceptor.JwtBlacklistInterceptor;
import com.youthfi.auth.global.resolver.CurrentUserArgumentResolver;
import com.youthfi.auth.global.resolver.RefreshTokenArgumentResolver;
import com.youthfi.auth.global.security.ExcludeBlacklistPathProperties;
import com.youthfi.auth.global.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenProvider tokenProvider;
    private final JwtBlacklistInterceptor jwtBlacklistInterceptor;
    private final ExcludeBlacklistPathProperties excludeBlacklistPathProperties;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.addAll(List.of(
                new CurrentUserArgumentResolver(tokenProvider),
                new RefreshTokenArgumentResolver(tokenProvider)
        ));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtBlacklistInterceptor)
                .excludePathPatterns(excludeBlacklistPathProperties.getExcludeAuthPaths());
    }
}