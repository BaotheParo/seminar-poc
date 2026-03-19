package com.foodstreet.voice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
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
                        "http://192.168.*.*:*"  // Cho phép mọi thiết bị trong mạng LAN
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String localPath = audioProperties.getLocalPath();
        // Đảm bảo path kết thúc bằng slash
        if (!localPath.endsWith("/")) {
            localPath = localPath + "/";
        }
        registry.addResourceHandler("/audio/**")
                .addResourceLocations("file:" + localPath)
                .setCachePeriod(3600);

        System.out.println("[AudioConfig] /audio/** -> file:" + localPath);
    }

}
