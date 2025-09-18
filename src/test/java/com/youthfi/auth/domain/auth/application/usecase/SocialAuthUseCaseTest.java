package com.youthfi.auth.domain.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.youthfi.auth.domain.auth.application.dto.response.LoginResponse;
import com.youthfi.auth.domain.auth.domain.service.SocialOAuthService;
import com.youthfi.auth.global.exception.RestApiException;
import com.youthfi.auth.global.exception.code.status.AuthErrorStatus;

@ExtendWith(MockitoExtension.class)
class SocialAuthUseCaseTest {

    @Mock
    private SocialOAuthService socialOAuthService;

    @InjectMocks
    private SocialAuthUseCase socialAuthUseCase;

    @Test
    @DisplayName("소셜 로그인 성공 시 서비스 결과(LoginResponse)를 그대로 반환한다")
    void signInOrSignUp_success() {
        // given
        String provider = "google";
        String code = "auth-code-123";
        LoginResponse expected = new LoginResponse("access-token", "refresh-token");
        when(socialOAuthService.signInOrSignUp(eq(provider), eq(code))).thenReturn(expected);

        // when
        LoginResponse actual = socialAuthUseCase.signInOrSignUp(provider, code);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.accessToken()).isEqualTo("access-token");
        assertThat(actual.refreshToken()).isEqualTo("refresh-token");
        verify(socialOAuthService).signInOrSignUp(eq(provider), eq(code));
    }

    @Test
    @DisplayName("미지원 공급자 등 서비스 예외가 발생하면 그대로 전파한다")
    void signInOrSignUp_throwsFromService() {
        // given
        String provider = "unknown";
        String code = "auth-code-xyz";
        when(socialOAuthService.signInOrSignUp(eq(provider), eq(code)))
                .thenThrow(new RestApiException(AuthErrorStatus.SOCIAL_UNSUPPORTED_PROVIDER));

        // expect
        assertThatThrownBy(() -> socialAuthUseCase.signInOrSignUp(provider, code))
                .isInstanceOf(RestApiException.class);
        verify(socialOAuthService).signInOrSignUp(eq(provider), eq(code));
    }
}


