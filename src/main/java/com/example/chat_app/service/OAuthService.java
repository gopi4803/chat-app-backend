package com.example.chat_app.service;

import com.example.chat_app.model.User;
import com.example.chat_app.repository.UserRepository;
import com.example.chat_app.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate;

    @Value("${app.oauth.google.client-id}")
    private String googleClientId;

    @Value("${app.oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${app.oauth.google.redirect-uri}")
    private String googleRedirectUri;

//    Builds the Google authorization URL with a state parameter for CSRF protection.
    public String buildGoogleAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile email")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

//  Handles Google OAuth callback — exchanges code for tokens, retrieves user info, and returns the User.
    public User handleGoogleCallback(String code) {
        //  Exchange code for Google access token
        String tokenEndpoint = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = UriComponentsBuilder.newInstance()
                .queryParam("code", code)
                .queryParam("client_id", googleClientId)
                .queryParam("client_secret", googleClientSecret)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("grant_type", "authorization_code")
                .build()
                .toUriString()
                .substring(1); // remove '?'

        HttpEntity<String> tokenRequest = new HttpEntity<>(body, headers);

        ResponseEntity<Map> tokenResponse =
                restTemplate.exchange(tokenEndpoint, HttpMethod.POST, tokenRequest, Map.class);

        String accessToken = (String) tokenResponse.getBody().get("access_token");

        //  Fetch Google user info
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);

        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                Map.class
        );

        Map<String, Object> userInfo = userResponse.getBody();
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String googleId = (String) userInfo.get("sub");

        //  Create or update user in DB
        Optional<User> existingUser = userRepository.findByEmail(email);
        return existingUser.orElseGet(() -> {
            User newUser = new User();
            newUser.setUsername(name);
            newUser.setEmail(email);
            newUser.setOauthProvider("google");
            newUser.setOauthProviderId(googleId);
            newUser.setPasswordHash(""); // no password for OAuth users
            return userRepository.save(newUser);
        });
    }
}
