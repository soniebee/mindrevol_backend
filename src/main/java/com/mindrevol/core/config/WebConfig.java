package com.mindrevol.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Cấu hình CORS chính hiện đã được xử lý trong SecurityConfig.corsConfigurationSource()
        // WebMvcConfigurer.addCorsMappings() thường chạy sau hoặc song song với Spring Security.
        // Để tránh xung đột, chúng ta có thể để trống ở đây hoặc chỉ dùng cho các endpoint không qua Security.
    }
}