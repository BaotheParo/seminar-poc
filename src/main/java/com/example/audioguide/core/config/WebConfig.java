package com.example.audioguide.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình để serve file tĩnh (âm thanh audio/mp3).
 * Giúp trình duyệt có thể truy cập file thông qua URL, ví dụ:
 * http://localhost:8080/media/oc_oanh.mp3
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map tất cả request bắt đầu bằng /media/**
        registry.addResourceHandler("/media/**")
                // Trỏ về thư mục vật lý trên máy tính (đường dẫn tương đối từ thư mục gốc
                // project)
                // file:./src/main/resources/static/audio/ nghĩa là nó sẽ tìm trong folder
                // source code
                .addResourceLocations("file:src/main/resources/static/audio/");
    }
}
