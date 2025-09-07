package com.youthfi.auth.domain.email.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.youthfi.auth.domain.email.application.dto.request.VerifyEmailRequest;
import com.youthfi.auth.domain.email.application.dto.response.EmailVerificationResponse;
import com.youthfi.auth.domain.email.domain.service.EmailService;
import com.youthfi.auth.domain.email.domain.service.EmailVerificationService;
import com.youthfi.auth.global.exception.RestApiException;
import com.youthfi.auth.global.exception.code.status.EmailErrorStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyEmailUseCase 테스트")
class VerifyEmailUseCaseTest {

    @Mock
    private EmailService emailService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private VerifyEmailUseCase verifyEmailUseCase;

    private VerifyEmailRequest validRequest;
    private final String VALID_TOKEN = "valid.jwt.token";
    private final String VALID_EMAIL = "test@example.com";
    private final long VERIFICATION_TTL_SECONDS = 1800L;

    @BeforeEach
    void setUp() {
        validRequest = new VerifyEmailRequest(VALID_TOKEN);
    }

    @Test
    @DisplayName("이메일 인증 검증 성공")
    void verifyEmail_Success() {
        // given
        when(emailService.verifySignupToken(VALID_TOKEN)).thenReturn(true);
        when(emailService.getEmailFromToken(VALID_TOKEN)).thenReturn(VALID_EMAIL);
        doNothing().when(emailVerificationService).markEmailAsVerified(anyString(), anyLong());

        // when
        EmailVerificationResponse response = verifyEmailUseCase.verifyEmail(validRequest);

        // then
        assertNotNull(response);
        assertTrue(response.verified());
        assertEquals(VERIFICATION_TTL_SECONDS, response.expiresInSec());

        verify(emailService, times(1)).verifySignupToken(VALID_TOKEN);
        verify(emailService, times(1)).getEmailFromToken(VALID_TOKEN);
        verify(emailVerificationService, times(1)).markEmailAsVerified(VALID_EMAIL, VERIFICATION_TTL_SECONDS);
    }

    @Test
    @DisplayName("JWT 토큰 검증 실패 시 EMAIL_INVALID_TOKEN 예외 발생")
    void verifyEmail_InvalidToken_ThrowsException() {
        // given
        when(emailService.verifySignupToken(VALID_TOKEN))
                .thenThrow(new IllegalArgumentException("유효하지 않은 인증 토큰입니다."));

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            verifyEmailUseCase.verifyEmail(validRequest);
        });

        assertEquals(EmailErrorStatus.EMAIL_INVALID_TOKEN.getCode(), exception.getErrorCode());
        verify(emailService, times(1)).verifySignupToken(VALID_TOKEN);
        verify(emailService, never()).getEmailFromToken(anyString());
        verify(emailVerificationService, never()).markEmailAsVerified(anyString(), anyLong());
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 실패 시 EMAIL_INVALID_TOKEN 예외 발생")
    void verifyEmail_EmailExtractionFailed_ThrowsException() {
        // given
        when(emailService.verifySignupToken(VALID_TOKEN)).thenReturn(true);
        when(emailService.getEmailFromToken(VALID_TOKEN)).thenThrow(new RuntimeException("토큰에서 이메일 추출 실패"));

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            verifyEmailUseCase.verifyEmail(validRequest);
        });

        assertEquals(EmailErrorStatus.EMAIL_INVALID_TOKEN.getCode(), exception.getErrorCode());
        verify(emailService, times(1)).verifySignupToken(VALID_TOKEN);
        verify(emailService, times(1)).getEmailFromToken(VALID_TOKEN);
        verify(emailVerificationService, never()).markEmailAsVerified(anyString(), anyLong());
    }

    @Test
    @DisplayName("이메일 인증 상태 저장 실패 시 EMAIL_VERIFICATION_FAILED 예외 발생")
    void verifyEmail_VerificationServiceFailed_ThrowsException() {
        // given
        when(emailService.verifySignupToken(VALID_TOKEN)).thenReturn(true);
        when(emailService.getEmailFromToken(VALID_TOKEN)).thenReturn(VALID_EMAIL);
        doThrow(new RuntimeException("Redis 저장 실패"))
                .when(emailVerificationService).markEmailAsVerified(anyString(), anyLong());

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            verifyEmailUseCase.verifyEmail(validRequest);
        });

        assertEquals(EmailErrorStatus.EMAIL_VERIFICATION_FAILED.getCode(), exception.getErrorCode());
        verify(emailService, times(1)).verifySignupToken(VALID_TOKEN);
        verify(emailService, times(1)).getEmailFromToken(VALID_TOKEN);
        verify(emailVerificationService, times(1)).markEmailAsVerified(VALID_EMAIL, VERIFICATION_TTL_SECONDS);
    }

    @Test
    @DisplayName("기타 예외 발생 시 EMAIL_VERIFICATION_FAILED 예외 발생")
    void verifyEmail_OtherException_ThrowsException() {
        // given
        when(emailService.verifySignupToken(VALID_TOKEN))
                .thenThrow(new RuntimeException("알 수 없는 오류"));

        // when & then
        RestApiException exception = assertThrows(RestApiException.class, () -> {
            verifyEmailUseCase.verifyEmail(validRequest);
        });

        assertEquals(EmailErrorStatus.EMAIL_VERIFICATION_FAILED.getCode(), exception.getErrorCode());
        verify(emailService, times(1)).verifySignupToken(VALID_TOKEN);
        verify(emailService, never()).getEmailFromToken(anyString());
        verify(emailVerificationService, never()).markEmailAsVerified(anyString(), anyLong());
    }

    @Test
    @DisplayName("다양한 토큰으로 테스트")
    void verifyEmail_DifferentTokens() {
        // given
        String[] testTokens = {
                "token1.jwt.signature",
                "token2.jwt.signature",
                "token3.jwt.signature"
        };

        when(emailService.verifySignupToken(anyString())).thenReturn(true);
        when(emailService.getEmailFromToken(anyString())).thenReturn(VALID_EMAIL);
        doNothing().when(emailVerificationService).markEmailAsVerified(anyString(), anyLong());

        // when & then
        for (String token : testTokens) {
            VerifyEmailRequest request = new VerifyEmailRequest(token);
            
            EmailVerificationResponse response = verifyEmailUseCase.verifyEmail(request);
            
            assertNotNull(response);
            assertTrue(response.verified());
            assertEquals(VERIFICATION_TTL_SECONDS, response.expiresInSec());
        }

        verify(emailService, times(testTokens.length)).verifySignupToken(anyString());
        verify(emailService, times(testTokens.length)).getEmailFromToken(anyString());
        verify(emailVerificationService, times(testTokens.length)).markEmailAsVerified(anyString(), anyLong());
    }

    @Test
    @DisplayName("빈 토큰으로 테스트")
    void verifyEmail_EmptyToken() {
        // given
        VerifyEmailRequest emptyTokenRequest = new VerifyEmailRequest("");

        // when & then
        assertThrows(Exception.class, () -> {
            verifyEmailUseCase.verifyEmail(emptyTokenRequest);
        });
    }

    @Test
    @DisplayName("null 토큰으로 테스트")
    void verifyEmail_NullToken() {
        // given
        VerifyEmailRequest nullTokenRequest = new VerifyEmailRequest(null);

        // when & then
        assertThrows(Exception.class, () -> {
            verifyEmailUseCase.verifyEmail(nullTokenRequest);
        });
    }
}
