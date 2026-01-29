package com.linkwave.app.service.auth;

import com.linkwave.app.config.auth.EmailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails via SMTP.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;
    
    // Flag to skip actual email sending in dev mode (set via profile or env)
    private final boolean devMode;

    public EmailService(JavaMailSender mailSender, EmailConfig emailConfig,
                        @org.springframework.beans.factory.annotation.Value("${linkwave.mail.dev-mode:true}") boolean devMode) {
        this.mailSender = mailSender;
        this.emailConfig = emailConfig;
        this.devMode = devMode;
    }

    /**
     * Send OTP code via email.
     * 
     * @param to recipient email address
     * @param otpCode the OTP code to send
     * @throws EmailDeliveryException if email sending fails
     */
    public void sendOtpEmail(String to, String otpCode) {
        // In dev mode, just log the OTP instead of sending email
        if (devMode) {
            log.info("╔══════════════════════════════════════════╗");
            log.info("║  DEV MODE - OTP CODE: {}                ║", otpCode);
            log.info("║  For: {}                                 ", to);
            log.info("╚══════════════════════════════════════════╝");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddress = emailConfig.getFrom();
            if (fromAddress == null || fromAddress.isBlank()) {
                fromAddress = "no-reply@linkwave.app";
            }
            
            log.info("Attempting to send OTP email to: {} from: {}", maskEmail(to), fromAddress);
            
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Your Linkwave OTP Code");
            helper.setText(buildOtpEmailBody(otpCode), true);

            mailSender.send(message);
            
            log.info("OTP email sent successfully to: {}", maskEmail(to));
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {} - MessagingException: {}", maskEmail(to), e.getMessage(), e);
            throw new EmailDeliveryException("Failed to send OTP email", e);
        } catch (Exception e) {
            log.error("Unexpected error while sending OTP email to: {} - Exception type: {} Message: {}", 
                maskEmail(to), e.getClass().getName(), e.getMessage(), e);
            throw new EmailDeliveryException("Failed to send OTP email", e);
        }
    }

    /**
     * Build HTML email body for OTP.
     */
    private String buildOtpEmailBody(String otpCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #2563eb; 
                                letter-spacing: 5px; text-align: center; 
                                padding: 20px; background: #f3f4f6; 
                                border-radius: 8px; margin: 20px 0; }
                    .footer { font-size: 12px; color: #6b7280; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Your Linkwave OTP Code</h2>
                    <p>Use the following code to complete your authentication:</p>
                    <div class="otp-code">%s</div>
                    <p>This code will expire in 5 minutes.</p>
                    <p>If you didn't request this code, please ignore this email.</p>
                    <div class="footer">
                        <p>This is an automated message from Linkwave. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(otpCode);
    }

    /**
     * Mask email address for logging (show first 2 chars and domain).
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String maskedLocal = localPart.length() > 2 
            ? localPart.substring(0, 2) + "***" 
            : "***";
        return maskedLocal + "@" + parts[1];
    }

    /**
     * Exception thrown when email delivery fails.
     */
    public static class EmailDeliveryException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
