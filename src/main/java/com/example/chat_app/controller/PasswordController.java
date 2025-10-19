package com.example.chat_app.controller;

import com.example.chat_app.dto.*;
import com.example.chat_app.service.PasswordResetService;
import com.example.chat_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordResetService passwordResetService;
    private final UserService userService;
    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        String baseUrl = this.frontendBaseUrl != null ? this.frontendBaseUrl : "http://localhost:5173";
        passwordResetService.createPasswordResetToken(email, baseUrl);

        // Always return 200 with a generic message
        return ResponseEntity.ok(Map.of("message", "If that email exists, a password reset link has been sent."));
    }


    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        if (token == null || newPassword == null ||
                token.isBlank() || newPassword.isBlank() || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid Token or new Password"));
        }

        try {
            passwordResetService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Unable to reset password"));
        }
    }
}
