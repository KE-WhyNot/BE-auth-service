package com.youthfi.auth.domain.auth.domain.entity;

import com.youthfi.auth.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_provider_provider_user_id", columnNames = {"social_provider", "provider_user_id"})
    }
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String birth;

    // 소셜 로그인 관련 필드 (옵션)
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider")
    private SocialProvider socialProvider; // GOOGLE, NAVER, KAKAO 등

    @Column(name = "provider_user_id")
    private String providerUserId; // 공급자 측 고유 ID

    @Column
    private Boolean emailVerified; // 공급자에서 이메일 검증 여부

    @Column
    private String profileImageUrl; // 프로필 이미지 URL

    public void linkSocial(SocialProvider provider, String providerUserId, Boolean emailVerified, String profileImageUrl) {
        this.socialProvider = provider;
        this.providerUserId = providerUserId;
        this.emailVerified = emailVerified;
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfile(String name, String birth, String encodedNewPassword) {
        this.name = name;
        this.birth = birth;
        if (encodedNewPassword != null && !encodedNewPassword.isBlank()) {
            this.password = encodedNewPassword;
        }
    }
}