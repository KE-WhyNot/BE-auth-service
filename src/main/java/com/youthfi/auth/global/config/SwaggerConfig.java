package com.youthfi.auth.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        String schemeName = "Bearer Authentication";

        // 1) SecurityScheme 정의
        Components components = new Components()
                .addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                );

        // 2) 전역 SecurityRequirement 추가
        SecurityRequirement requirement = new SecurityRequirement()
                .addList(schemeName);

        return new OpenAPI()
                .components(components)
                .addSecurityItem(requirement)
                .info(new Info()
                        .title("Youth-Fi API")
                        .description("카카오엔터프라이즈 Youth-Fi 웹앱 백엔드 API")
                        .version("1.0.0")
                );
    }
}