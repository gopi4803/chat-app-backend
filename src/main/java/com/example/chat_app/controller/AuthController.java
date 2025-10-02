package com.example.chat_app.controller;

import com.example.chat_app.dto.LoginResponse;
import com.example.chat_app.dto.UserResponse;
import com.example.chat_app.model.User;
import com.example.chat_app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.chat_app.dto.LoginRequest;
import com.example.chat_app.dto.SignupRequest;
import com.example.chat_app.security.JwtUtils;


@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
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
        String token=jwtUtils.generateJwtToken(user.getEmail());
        return ResponseEntity.ok(new LoginResponse(token, UserResponse.fromUser(user)));
    }
}


