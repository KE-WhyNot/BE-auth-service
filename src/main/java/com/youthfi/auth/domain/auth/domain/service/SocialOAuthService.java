package com.youthfi.auth.domain.auth.domain.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.youthfi.auth.domain.auth.application.dto.response.GoogleProfileResponse;
import com.youthfi.auth.domain.auth.application.dto.response.KakaoProfileResponse;
import com.youthfi.auth.domain.auth.application.dto.response.LoginResponse;
import com.youthfi.auth.domain.auth.application.dto.response.NaverProfileResponse;
import com.youthfi.auth.domain.auth.domain.entity.SocialProvider;
import com.youthfi.auth.domain.auth.domain.entity.User;
import com.youthfi.auth.domain.auth.domain.repository.UserRepository;
import com.youthfi.auth.global.config.properties.OAuthClientProperties;
import com.youthfi.auth.global.config.properties.OAuthProviderProperties;
import com.youthfi.auth.global.exception.RestApiException;
import com.youthfi.auth.global.exception.code.status.AuthErrorStatus;
import com.youthfi.auth.global.security.TokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialOAuthService {

    private final OAuthProviderProperties providerProps;
    private final OAuthClientProperties clientProps;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final RestClient restClient = RestClient.create();

    public LoginResponse signInOrSignUp(String providerKey, String code) {
        String key = providerKey.toLowerCase();
        return switch (key) {
            case "google" -> signInOrSignUpGoogle(code);
            case "naver" -> signInOrSignUpNaver(code);
            case "kakao" -> signInOrSignUpKakao(code);
            default -> throw new RestApiException(AuthErrorStatus.SOCIAL_UNSUPPORTED_PROVIDER);
        };
    }

    private LoginResponse signInOrSignUpGoogle(String code) {
        var provider = providerProps.getProviders().get("google");
        var client = clientProps.getClients().get("google");
        String accessToken = exchangeToken(provider.getTokenUri(), client.getClientId(), client.getClientSecret(), client.getRedirectUri(), code);

        GoogleProfileResponse r = restClient.get()
                .uri(provider.getUserInfoUri())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(GoogleProfileResponse.class);
        if (r == null || r.sub() == null) {
            throw new RestApiException(AuthErrorStatus.SOCIAL_USERINFO_FAILED);
        }

        String providerUserId = r.sub();
        User user = userRepository.findBySocialProviderAndProviderUserId(SocialProvider.GOOGLE, providerUserId)
                .orElseGet(() -> upsertByEmailOrCreate(
                        r.email(),
                        User.builder()
                                .userId("google:" + providerUserId)
                                .email(r.email())
                                .password("")
                                .name(r.name() != null ? r.name() : ("google:" + providerUserId))
                                .birth("")
                                .socialProvider(SocialProvider.GOOGLE)
                                .providerUserId(providerUserId)
                                .emailVerified(r.email_verified())
                                .profileImageUrl(r.picture())
                                .build(),
                        SocialProvider.GOOGLE,
                        providerUserId,
                        r.email_verified(),
                        r.picture()
                ));
        return issueTokens(user.getUserId());
    }

    private LoginResponse signInOrSignUpNaver(String code) {
        var provider = providerProps.getProviders().get("naver");
        var client = clientProps.getClients().get("naver");
        String accessToken = exchangeToken(provider.getTokenUri(), client.getClientId(), client.getClientSecret(), client.getRedirectUri(), code);

        NaverProfileResponse r = restClient.get()
                .uri(provider.getUserInfoUri())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(NaverProfileResponse.class);
        if (r == null || r.response() == null || r.response().id() == null) {
            throw new RestApiException(AuthErrorStatus.SOCIAL_USERINFO_FAILED);
        }

        var resp = r.response();
        String providerUserId = resp.id();
        String name = resp.name() != null ? resp.name() : resp.nickname();
        User user = userRepository.findBySocialProviderAndProviderUserId(SocialProvider.NAVER, providerUserId)
                .orElseGet(() -> upsertByEmailOrCreate(
                        resp.email(),
                        User.builder()
                                .userId("naver:" + providerUserId)
                                .email(resp.email())
                                .password("")
                                .name(name != null ? name : ("naver:" + providerUserId))
                                .birth("")
                                .socialProvider(SocialProvider.NAVER)
                                .providerUserId(providerUserId)
                                .emailVerified(null)
                                .profileImageUrl(resp.profile_image())
                                .build(),
                        SocialProvider.NAVER,
                        providerUserId,
                        null,
                        resp.profile_image()
                ));
        return issueTokens(user.getUserId());
    }

    private LoginResponse signInOrSignUpKakao(String code) {
        var provider = providerProps.getProviders().get("kakao");
        var client = clientProps.getClients().get("kakao");
        String accessToken = exchangeToken(provider.getTokenUri(), client.getClientId(), client.getClientSecret(), client.getRedirectUri(), code);

        KakaoProfileResponse r = restClient.get()
                .uri(provider.getUserInfoUri())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoProfileResponse.class);
        if (r == null || r.id() == null) {
            throw new RestApiException(AuthErrorStatus.SOCIAL_USERINFO_FAILED);
        }

        var acc = r.kakao_account();
        String providerUserId = String.valueOf(r.id());
        String name = acc != null && acc.profile() != null ? acc.profile().nickname() : null;
        String image = acc != null && acc.profile() != null ? acc.profile().profile_image_url() : null;
        User user = userRepository.findBySocialProviderAndProviderUserId(SocialProvider.KAKAO, providerUserId)
                .orElseGet(() -> upsertByEmailOrCreate(
                        acc != null ? acc.email() : null,
                        User.builder()
                                .userId("kakao:" + providerUserId)
                                .email(acc != null ? acc.email() : null)
                                .password("")
                                .name(name != null ? name : ("kakao:" + providerUserId))
                                .birth("")
                                .socialProvider(SocialProvider.KAKAO)
                                .providerUserId(providerUserId)
                                .emailVerified(acc != null ? acc.is_email_verified() : null)
                                .profileImageUrl(image)
                                .build(),
                        SocialProvider.KAKAO,
                        providerUserId,
                        acc != null ? acc.is_email_verified() : null,
                        image
                ));
        return issueTokens(user.getUserId());
    }

    private LoginResponse issueTokens(String userId) {
        String access = tokenProvider.createAccessToken(userId);
        String refresh = tokenProvider.createRefreshToken(userId);
        Duration ttl = tokenProvider.getRemainingDuration(refresh).orElse(Duration.ofDays(14));
        refreshTokenService.saveRefreshToken(userId, refresh, ttl);
        return new LoginResponse(access, refresh);
    }

    private String exchangeToken(String tokenUri, String clientId, String clientSecret, String redirectUri, String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        Map<?, ?> tokenResponse = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        Object token = tokenResponse != null ? tokenResponse.get("access_token") : null;
        if (token == null) {
            throw new RestApiException(AuthErrorStatus.SOCIAL_TOKEN_EXCHANGE_FAILED);
        }
        return token.toString();
    }

    private User upsertByEmailOrCreate(String email, User toCreate,
                                       SocialProvider provider, String providerUserId,
                                       Boolean emailVerified, String profileImageUrl) {
        if (email != null) {
            return userRepository.findByEmail(email)
                    .map(existing -> {
                        existing.linkSocial(provider, providerUserId, emailVerified, profileImageUrl);
                        return userRepository.save(existing);
                    })
                    .orElseGet(() -> userRepository.save(toCreate));
        }
        return userRepository.save(toCreate);
    }
}


