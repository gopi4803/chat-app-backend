package com.example.chat_app.controller;

import com.example.chat_app.dto.*;
import com.example.chat_app.model.User;
import com.example.chat_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.chat_app.security.JwtUtils;

import java.util.List;


@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/sign-up")
    public ResponseEntity<UserResponse> signup(@RequestBody SignupRequest request) {
        User newUser = userService.registerUser(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok(UserResponse.fromUser(newUser));
    }

    @PostMapping("/log-in")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userService.authenticateUser(request.getEmail(), request.getPassword());
        String accessToken=jwtUtils.generateAccessToken(user.getEmail());
        String refreshToken= jwtUtils.generateRefreshToken(user.getEmail());
        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, UserResponse.fromUser(user)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (jwtUtils.validateJwtToken(refreshToken)) {
            String email = jwtUtils.getEmailFromJwtToken(refreshToken);
            String newAccessToken = jwtUtils.generateAccessToken(email);
            return ResponseEntity.ok(new LoginResponse(
                    newAccessToken,
                    refreshToken,
                    userService.getUserByEmail(email).map(UserResponse::fromUser).orElse(null)
            ));
        }
        return ResponseEntity.badRequest().body("Invalid refresh token");
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


