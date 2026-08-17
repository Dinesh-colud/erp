package com.erp.service.impl;

import com.erp.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.verify-email-url}")
    private String verifyEmailUrl;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String link = verifyEmailUrl + "?token=" + token;
        String body = "Hi " + firstName + ",\n\nPlease verify your email by visiting:\n" + link
                + "\n\nIf you did not create this account, please ignore this email.";
        send(toEmail, "Verify your College ERP account", body);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        String link = resetPasswordUrl + "?token=" + token;
        String body = "Hi " + firstName + ",\n\nWe received a request to reset your password. Visit:\n" + link
                + "\n\nThis link expires in 30 minutes. If you did not request this, please ignore this email.";
        send(toEmail, "Reset your College ERP password", body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // Email delivery must never break the calling business transaction.
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
