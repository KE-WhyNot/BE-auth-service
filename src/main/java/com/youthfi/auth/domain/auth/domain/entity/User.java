package com.youthfi.auth.domain.auth.domain.entity;

import com.youthfi.auth.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
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

    public void updateProfile(String name, String birth, String encodedNewPassword) {
        this.name = name;
        this.birth = birth;
        if (encodedNewPassword != null && !encodedNewPassword.isBlank()) {
            this.password = encodedNewPassword;
        }
    }
}