package com.example.chat_app.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
