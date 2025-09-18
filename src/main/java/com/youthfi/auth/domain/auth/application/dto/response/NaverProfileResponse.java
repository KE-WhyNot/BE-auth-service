package com.youthfi.auth.domain.auth.application.dto.response;

/**
 * Naver 사용자 정보 응답 DTO.
 * 최상위 response 필드 내부에 실제 사용자 정보가 위치합니다.
 */
public record NaverProfileResponse(
        NaverResponse response
) {
    /** Naver의 실제 사용자 정보 중첩 오브젝트 */
    public record NaverResponse(
            String id,
            String email,
            String name,
            String nickname,
            String profile_image
    ) {}
}


