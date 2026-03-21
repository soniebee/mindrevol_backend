package com.mindrevol.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
public class MindrevolBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MindrevolBackendApplication.class, args);
	}

	// --- THÊM ĐOẠN NÀY ---
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	// ---------------------
}