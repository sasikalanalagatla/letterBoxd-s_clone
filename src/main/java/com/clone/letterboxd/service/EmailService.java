package com.clone.letterboxd.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        log.info("Preparing to send OTP email to {} in a background thread", toEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("sasinalagatla30@gmail.com", "Letterboxd Clone");
            helper.setTo(toEmail);
            helper.setSubject("Letterboxd - Password Reset OTP");

            // Plain text version
            String plainTextBody = "Hello,\n\n"
                    + "Your OTP for password reset is: " + otp + "\n\n"
                    + "This code expires in 1 hour.\n\n"
                    + "If you didn't request this, please ignore this email.\n\n"
                    + "Best regards,\n"
                    + "Letterboxd Team";

            // HTML version
            String htmlBody = "<html><body style='font-family: Arial, sans-serif;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>"
                    + "<h2 style='color: #2c3e50; text-align: center;'>Password Reset OTP</h2>"
                    + "<p>Hello,</p>"
                    + "<p>We received a request to reset your password. Use the code below to verify your identity:</p>"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "<span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #008100; padding: 10px 20px; background: #f9f9f9; border-radius: 5px;'>" 
                    + otp + "</span>"
                    + "</div>"
                    + "<p>This code will expire in <strong>1 hour</strong>.</p>"
                    + "<p>If you didn't request this, please ignore this email.</p>"
                    + "<p>Best regards,<br><strong>Letterboxd Team</strong></p>"
                    + "</div>"
                    + "</body></html>";

            helper.setText(plainTextBody, htmlBody);

            mailSender.send(message);
            log.info("OTP email successfully sent to {} via JavaMailSender", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {} via JavaMailSender", toEmail, e);
        }
    }
}
