package com.foodstreet.voice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers (ResourceHandlerRegistry registry){
        //Tao duong dan uploads de dua audio vao
        Path uploadDir = Paths.get("./uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        // Khi goi api ten file do se tim thay o thu muc audio
        registry.addResourceHandler("/audio/**")
                .addResourceLocations("file: "+ uploadPath + "/audio/");

        System.out.println("/audio/** -> "+ uploadPath +"/audio/");
    }

}
