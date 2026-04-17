package com.mindrevol.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MindrevolBackendCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(MindrevolBackendCoreApplication.class, args);
	}

}