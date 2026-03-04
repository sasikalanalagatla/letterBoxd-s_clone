package com.clone.letterboxd.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (sendGridApiKey == null || sendGridApiKey.isBlank()) {
            log.warn("SendGrid API key not configured. Reset link (console only): {}", resetLink);
            return;
        }

        try {
            Email from = new Email("sasinalagatla30@gmail.com", "Letterboxd Clone");
            Email to = new Email(toEmail);
            String subject = "Letterboxd - Password Reset Request";
            String body = "Hello,\n\n"
                    + "We received a request to reset your password. Click the link below to set a new password:\n\n"
                    + resetLink + "\n\n"
                    + "This link expires in 1 hour.\n\n"
                    + "If you didn't request this, please ignore this email.\n\n"
                    + "Best regards,\n"
                    + "Letterboxd Team";

            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Password reset email sent to {} via SendGrid (status: {})", toEmail, response.getStatusCode());
            } else {
                log.error("SendGrid returned error status {} for {}: {}", response.getStatusCode(), toEmail, response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to send password reset email to {} via SendGrid", toEmail, e);
        }
    }
}
