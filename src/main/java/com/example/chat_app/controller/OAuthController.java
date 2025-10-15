package com.example.chat_app.controller;

import com.example.chat_app.model.User;
import com.example.chat_app.service.OAuthService;
import com.example.chat_app.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oauthService;
    private final JwtUtils jwtUtils;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;


//   Returns Google login URL for frontend redirection.
    @GetMapping("/google/login")
    public ResponseEntity<String> googleLogin() {
        String state = "random-generated-state";
        return ResponseEntity.ok(oauthService.buildGoogleAuthorizationUrl(state));
    }

//  Handles Google OAuth callback.
    @GetMapping("/callback/google")
    public ResponseEntity<Void> googleCallback(@RequestParam("code") String code,
                                               @RequestParam(required = false) String state,
                                               HttpServletResponse response) {
        try {
            User user = oauthService.handleGoogleCallback(code);

            String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(false)
                    .path("/")
                    .maxAge(jwtUtils.getRefreshExpirationSeconds())
                    .sameSite("Strict")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // Redirect to frontend (access token is obtained via /refresh-token)
            return ResponseEntity.status(302)
                    .header("Location", frontendBaseUrl + "/oauth2/redirect")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(302)
                    .header("Location", frontendBaseUrl + "/oauth-failure")
                    .build();
        }
    }
}
