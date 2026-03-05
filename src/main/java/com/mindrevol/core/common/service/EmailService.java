package com.mindrevol.core.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

/**
 * Service gửi email
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email đơn giản (text)
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@mindrevol.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    /**
     * Gửi email HTML
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("noreply@mindrevol.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(mimeMessage);
            log.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    /**
     * Gửi mã OTP qua email
     */
    @Async
    public void sendOtpEmail(String to, String otpCode, String fullname) {
        String subject = "Mã xác thực OTP - Mindrevol";

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #667eea; text-align: center; padding: 20px; background: white; border-radius: 8px; margin: 20px 0; letter-spacing: 5px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .warning { color: #e74c3c; font-size: 14px; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚀 Mindrevol</h1>
                        <p>Xác thực tài khoản của bạn</p>
                    </div>
                    <div class="content">
                        <p>Xin chào <strong>%s</strong>,</p>
                        <p>Cảm ơn bạn đã đăng ký tài khoản tại Mindrevol! Để hoàn tất quá trình đăng ký, vui lòng nhập mã OTP bên dưới:</p>
                        <div class="otp-code">%s</div>
                        <p class="warning">⚠️ Mã OTP này có hiệu lực trong <strong>15 phút</strong>. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                        <p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 Mindrevol. All rights reserved.</p>
                        <p>Email này được gửi tự động, vui lòng không trả lời.</p>
                    </div>
                </div>
            </body>
            </html>
            """, fullname != null ? fullname : "bạn", otpCode);

        sendHtmlEmail(to, subject, htmlContent);
    }
}

