package com.nibm.hr.hrms.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private JavaMailSender mailSender;

    public void sendWelcomeEmail(String to, String name, String username, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Welcome to DG5 HRMS - Your Account is Ready");

            String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                    + "<h2 style='color: #4f46e5;'>Welcome to the Team, " + name + "!</h2>"
                    + "<p>Your employee account has been successfully approved and activated by the administrator.</p>"
                    + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>"
                    + "<h4 style='margin-top: 0;'>Your Login Credentials:</h4>"
                    + "<p><strong>Username:</strong> " + username + "</p>"
                    + "<p><strong>Temporary Password:</strong> " + tempPassword + "</p>"
                    + "</div>"
                    + "<p style='color: #dc3545; font-size: 0.9em;'><strong>Security Note:</strong> Please log in to the system immediately and change this temporary password from the 'My Profile' section.</p>"
                    + "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'/>"
                    + "<p style='font-size: 0.8em; color: #777;'>This is an automated message from the DG5 HRMS System. Please do not reply to this email.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send welcome email: " + e.getMessage());
        }
    }
}