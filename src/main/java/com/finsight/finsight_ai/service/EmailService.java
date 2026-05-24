package com.finsight.finsight_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendBudgetWarningEmail(String toEmail, String userName,
                                       String category, double percentage,
                                       String spent, String limit) {
        String subject = "⚠️ Budget Warning — " + category + " at " +
                String.format("%.1f", percentage) + "%";

        String body = buildWarningEmailBody(
                userName, category, percentage, spent, limit);

        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendBudgetExceededEmail(String toEmail, String userName,
                                        String category,
                                        String spent, String limit) {
        String subject = "🚨 Budget Exceeded — " + category +
                " limit crossed!";

        String body = buildExceededEmailBody(
                userName, category, spent, limit);

        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = send as HTML

            mailSender.send(message);
            log.info("Email sent to: {} | Subject: {}", to, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to: {} | Error: {}",
                    to, e.getMessage());
        }
    }

    private String buildWarningEmailBody(String userName, String category,
                                         double percentage,
                                         String spent, String limit) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #FF8C00;">⚠️ Budget Warning</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>Your <strong>%s</strong> budget has reached
                       <strong style="color: #FF8C00;">%.1f%%</strong>.</p>
                    <table style="border-collapse: collapse; width: 300px;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;">
                                Spent</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">
                                ₹%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;">
                                Limit</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">
                                ₹%s</td>
                        </tr>
                    </table>
                    <p>You are approaching your limit.
                       Consider reducing %s expenses.</p>
                    <p style="color: #888; font-size: 12px;">
                        — FinSight AI</p>
                </body>
                </html>
                """.formatted(userName, category, percentage,
                spent, limit, category);
    }

    private String buildExceededEmailBody(String userName, String category,
                                          String spent, String limit) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #DC143C;">🚨 Budget Exceeded</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>You have <strong style="color: #DC143C;">exceeded</strong>
                       your <strong>%s</strong> budget.</p>
                    <table style="border-collapse: collapse; width: 300px;">
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;">
                                Spent</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">
                                ₹%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px; border: 1px solid #ddd;">
                                Limit</td>
                            <td style="padding: 8px; border: 1px solid #ddd;">
                                ₹%s</td>
                        </tr>
                    </table>
                    <p>Please review your spending and adjust your budget
                       if necessary.</p>
                    <p style="color: #888; font-size: 12px;">
                        — FinSight AI</p>
                </body>
                </html>
                """.formatted(userName, category, spent, limit);
    }
}