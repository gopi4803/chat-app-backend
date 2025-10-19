package com.example.chat_app.service;

import com.example.chat_app.model.PasswordResetToken;
import com.example.chat_app.model.User;
import com.example.chat_app.repository.PasswordResetTokenRepository;
import com.example.chat_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createPasswordResetToken(String email, String frontendBaseUrl) {
        userRepository.findByEmail(email).ifPresent(user -> {

            // Delete any existing token for the same user (to prevent DB constraint errors)
            tokenRepository.findByUser(user).ifPresent(existing -> tokenRepository.delete(existing));

            // Create a new token
            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(30))
                    .build();

            tokenRepository.save(prt);

            // Build secure reset link
            String link = String.format("%s/reset-password?token=%s", frontendBaseUrl, token);

            String subject = "Reset your password";
            String text = "You (or someone else) requested a password reset.\n\n" +
                    "Click below to reset your password (expires in 30 minutes):\n\n" +
                    link + "\n\n" +
                    "If you didn't request this, ignore this email.";

            emailService.sendSimpleMessage(user.getEmail(), subject, text);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepository.findByToken(token).orElse(null);

        if (prt == null || prt.isExpired()) {
            if (prt != null) {
                tokenRepository.delete(prt);
            }
            throw new IllegalArgumentException("Invalid password reset request");
        }

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(prt);
    }


    @Scheduled(cron = "0 0 * * * *") // every hour
    public void deleteExpiredTokens() {
        List<PasswordResetToken> expired = tokenRepository.findAll()
                .stream()
                .filter(PasswordResetToken::isExpired)
                .toList();
        if (!expired.isEmpty()) tokenRepository.deleteAll(expired);
    }
}
