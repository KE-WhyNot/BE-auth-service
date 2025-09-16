package com.youthfi.auth.domain.auth.application.dto.response;

/**
 * Kakao 사용자 정보 응답 DTO.
 * kakao_account와 profile 중첩 구조를 그대로 반영합니다.
 */
public record KakaoProfileResponse(
        Long id,
        KakaoAccount kakao_account
) {
    /** Kakao 계정 정보 서브오브젝트 */
    public record KakaoAccount(
            String email,
            Boolean is_email_verified,
            KakaoProfile profile
    ) {}

    /** Kakao 프로필 서브오브젝트 */
    public record KakaoProfile(
            String nickname,
            String profile_image_url
    ) {}
}


