package com.mindrevol.core.modules.notification.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
