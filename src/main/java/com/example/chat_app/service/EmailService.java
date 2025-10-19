package com.example.chat_app.service;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
}
