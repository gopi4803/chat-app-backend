package com.example.chat_app.controller;

import com.example.chat_app.dto.*;
import com.example.chat_app.model.User;
import com.example.chat_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.chat_app.security.JwtUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/sign-up")
    public ResponseEntity<UserResponse> signup(@RequestBody SignupRequest request) {
        User newUser = userService.registerUser(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(UserResponse.fromUser(newUser));
    }

    @PostMapping("/log-in")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        User user = userService.authenticateUser(request.getEmail(), request.getPassword());
        String accessToken = jwtUtils.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtUtils.getRefreshExpirationSeconds())
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Return access token in body, do NOT return refresh token to client
        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "user", UserResponse.fromUser(user)
        ));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || !jwtUtils.validateJwtToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing refresh token");
        }
        String email = jwtUtils.getEmailFromJwtToken(refreshToken);
        String newAccessToken = jwtUtils.generateAccessToken(email);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/log-out")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false) // change to true in production
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        List<UserResponse> allUsers = userService.getAllUsers()
                .stream()
                .map(UserResponse::fromUser)
                .toList();

        return ResponseEntity.ok(
                new DashboardResponse("Welcome to your dashboard!", allUsers)
        );
    }
    public static class DashboardResponse {
        private final String message;
        private final List<UserResponse> users;

        public DashboardResponse(String message, List<UserResponse> users) {
            this.message = message;
            this.users = users;
        }

        public String getMessage() { return message; }
        public List<UserResponse> getUsers() { return users; }
    }
}
