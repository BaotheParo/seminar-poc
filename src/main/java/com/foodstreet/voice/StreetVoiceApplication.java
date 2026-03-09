package com.foodstreet.voice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class StreetVoiceApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(StreetVoiceApplication.class, args);
	}

	private static void loadEnv() {
		try {
			java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
			if (java.nio.file.Files.exists(envPath)) {
				java.util.List<String> lines = java.nio.file.Files.readAllLines(envPath);
				for (String line : lines) {
					if (line.trim().isEmpty() || line.startsWith("#"))
						continue;
					String[] parts = line.split("=", 2);
					if (parts.length == 2) {
						String key = parts[0].trim();
						String value = parts[1].trim();
						System.setProperty(key, value);
					}
				}
				System.out.println("Loaded .env file successfully.");
			}
		} catch (Exception e) {
			System.err.println("Failed to load .env file: " + e.getMessage());
		}
	}

}