package com.example.Shipment.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PasswordResetEmailSimulator {

    private final String resetUrl;
    private final boolean logResetLink;

    public PasswordResetEmailSimulator(
            @Value("${app.password-reset.url:http://localhost:4200/reset-password}") String resetUrl,
            @Value("${app.password-reset.log-link:false}") boolean logResetLink) {
        this.resetUrl = resetUrl;
        this.logResetLink = logResetLink;
    }

    public void send(String email, String token) {
        if (!logResetLink) {
            log.info("Simulated password reset email queued for {}", email);
            return;
        }
        String separator = resetUrl.contains("?") ? "&" : "?";
        log.warn("Development-only simulated password reset link for {}: {}{}token={}",
                email, resetUrl, separator, token);
    }
}
