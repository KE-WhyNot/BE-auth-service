package com.youthfi.auth.domain.email.application.dto.request;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("VerifyEmailRequest 테스트")
class VerifyEmailRequestTest {

    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Test
    @DisplayName("유효한 토큰으로 생성 성공")
    void createWithValidToken_Success() {
        // given
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJFbWFpbFZlcmlmaWNhdGlvbiIsImlhdCI6MTYzMzQ1Njc4OSwiZXhwIjoxNjMzNDU3Nzg5LCJpZCI6InRlc3RAZXhhbXBsZS5jb20iLCJ0eXBlIjoic2lnbnVwIn0.signature";

        // when
        VerifyEmailRequest request = new VerifyEmailRequest(validToken);

        // then
        assertNotNull(request);
        assertEquals(validToken, request.verifyToken());
    }

    @Test
    @DisplayName("다양한 유효한 토큰 형식 테스트")
    void createWithVariousValidTokens_Success() {
        // given
        String[] validTokens = {
                "simple.token",
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJFbWFpbFZlcmlmaWNhdGlvbiJ9.signature",
                "header.payload.signature",
                "a.b.c",
                "very.long.token.with.many.parts.and.dots"
        };

        // when & then
        for (String token : validTokens) {
            VerifyEmailRequest request = new VerifyEmailRequest(token);
            assertNotNull(request);
            assertEquals(token, request.verifyToken());
        }
    }

    @Test
    @DisplayName("빈 문자열 토큰으로 생성 시 검증 실패")
    void createWithEmptyToken_ValidationFails() {
        // given
        String emptyToken = "";

        // when
        VerifyEmailRequest request = new VerifyEmailRequest(emptyToken);
        Set<ConstraintViolation<VerifyEmailRequest>> violations = validator.validate(request);

        // then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("인증 토큰은 필수입니다")));
    }

    @Test
    @DisplayName("null 토큰으로 생성 시 검증 실패")
    void createWithNullToken_ValidationFails() {
        // given
        String nullToken = null;

        // when
        VerifyEmailRequest request = new VerifyEmailRequest(nullToken);
        Set<ConstraintViolation<VerifyEmailRequest>> violations = validator.validate(request);

        // then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("인증 토큰은 필수입니다")));
    }

    @Test
    @DisplayName("공백만 있는 토큰으로 생성 시 검증 실패")
    void createWithWhitespaceOnlyToken_ValidationFails() {
        // given
        String whitespaceToken = "   ";

        // when
        VerifyEmailRequest request = new VerifyEmailRequest(whitespaceToken);
        Set<ConstraintViolation<VerifyEmailRequest>> violations = validator.validate(request);

        // then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("인증 토큰은 필수입니다")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "   ",
            "\t",
            "\n",
            "\r\n",
            " \t \n \r "
    })
    @DisplayName("다양한 공백 문자로만 구성된 토큰으로 생성 시 검증 실패")
    void createWithVariousWhitespaceTokens_ValidationFails(String whitespaceToken) {
        // when
        VerifyEmailRequest request = new VerifyEmailRequest(whitespaceToken);
        Set<ConstraintViolation<VerifyEmailRequest>> violations = validator.validate(request);

        // then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("인증 토큰은 필수입니다")));
    }

    @Test
    @DisplayName("매우 긴 토큰으로 생성 시 성공 (길이 제한 없음)")
    void createWithVeryLongToken_Success() {
        // given
        String longToken = "a".repeat(1000) + "." + "b".repeat(1000) + "." + "c".repeat(1000);

        // when
        VerifyEmailRequest request = new VerifyEmailRequest(longToken);

        // then
        assertNotNull(request);
        assertEquals(longToken, request.verifyToken());
    }

    @Test
    @DisplayName("특수 문자가 포함된 토큰으로 생성 시 성공")
    void createWithSpecialCharactersToken_Success() {
        // given
        String specialToken = "token+with-special_chars.and@symbols#and$more";

        // when
        VerifyEmailRequest request = new VerifyEmailRequest(specialToken);

        // then
        assertNotNull(request);
        assertEquals(specialToken, request.verifyToken());
    }

    @Test
    @DisplayName("Record equals 및 hashCode 테스트")
    void recordEqualsAndHashCode() {
        // given
        String token = "test.token";
        VerifyEmailRequest request1 = new VerifyEmailRequest(token);
        VerifyEmailRequest request2 = new VerifyEmailRequest(token);
        VerifyEmailRequest request3 = new VerifyEmailRequest("different.token");

        // when & then
        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    @DisplayName("Record toString 테스트")
    void recordToString() {
        // given
        String token = "test.token";
        VerifyEmailRequest request = new VerifyEmailRequest(token);

        // when
        String toString = request.toString();

        // then
        assertNotNull(toString);
        assertTrue(toString.contains(token));
    }

    @Test
    @DisplayName("JWT 형식 토큰으로 생성 시 성공")
    void createWithJWTFormatToken_Success() {
        // given
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJFbWFpbFZlcmlmaWNhdGlvbiIsImlhdCI6MTYzMzQ1Njc4OSwiZXhwIjoxNjMzNDU3Nzg5LCJpZCI6InRlc3RAZXhhbXBsZS5jb20iLCJ0eXBlIjoic2lnbnVwIn0.signature";

        // when
        VerifyEmailRequest request = new VerifyEmailRequest(jwtToken);

        // then
        assertNotNull(request);
        assertEquals(jwtToken, request.verifyToken());
    }
}
