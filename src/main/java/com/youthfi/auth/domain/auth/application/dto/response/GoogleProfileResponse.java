package com.youthfi.auth.domain.auth.application.dto.response;

/**
 * Google OpenID Connect UserInfo 응답을 담는 DTO.
 * 공급자 원본 JSON 스키마를 최대한 1:1로 유지합니다.
 */
public record GoogleProfileResponse(
        String sub,
        String email,
        Boolean email_verified,
        String name,
        String picture
) {}


