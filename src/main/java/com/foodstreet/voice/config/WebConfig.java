package com.foodstreet.voice.config;

import org.springframework.context.annotation.Bean;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AudioProperties audioProperties;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://192.168.*.*:*" // Cho phép mọi thiết bị trong mạng LAN
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String localPath = audioProperties.getResolvedLocalPath();
        registry.addResourceHandler("/audio/**")
                .addResourceLocations("file:" + localPath)
                // Audio files can be regenerated with the same filename after approval.
                // Disable aggressive caching so clients can fetch the newest bytes.
                .setCachePeriod(0);

        System.out.println("[AudioConfig] /audio/** -> file:" + localPath);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
