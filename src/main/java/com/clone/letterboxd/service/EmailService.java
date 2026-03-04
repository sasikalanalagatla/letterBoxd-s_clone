package com.clone.letterboxd.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
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

            // Plain text version
            String plainTextBody = "Hello,\n\n"
                    + "We received a request to reset your password. Click the link below to set a new password:\n\n"
                    + resetLink + "\n\n"
                    + "This link expires in 1 hour.\n\n"
                    + "If you didn't request this, please ignore this email.\n\n"
                    + "Best regards,\n"
                    + "Letterboxd Team";

            // HTML version (Better deliverability)
            String htmlBody = "<html><body>"
                    + "<h3>Password Reset Request</h3>"
                    + "<p>Hello,</p>"
                    + "<p>We received a request to reset your password for your Letterboxd account. Click the button below to set a new password:</p>"
                    + "<p><a href=\"" + resetLink + "\" style=\"background-color: #008100; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;\">Reset Password</a></p>"
                    + "<p>If the button doesn't work, copy and paste this link into your browser:</p>"
                    + "<p><a href=\"" + resetLink + "\">" + resetLink + "</a></p>"
                    + "<p>This link will expire in 1 hour.</p>"
                    + "<p>If you didn't request this, please ignore this email.</p>"
                    + "<p>Best regards,<br>Letterboxd Team</p>"
                    + "</body></html>";

            Mail mail = new Mail();
            mail.setFrom(from);
            mail.setSubject(subject);
            
            // Add both plain text and HTML
            Content textContent = new Content("text/plain", plainTextBody);
            Content htmlContent = new Content("text/html", htmlBody);
            mail.addContent(textContent);
            mail.addContent(htmlContent);

            Personalization personalization = new Personalization();
            personalization.addTo(to);
            mail.addPersonalization(personalization);

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
