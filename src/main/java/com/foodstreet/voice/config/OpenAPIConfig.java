package com.foodstreet.voice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI streetVoiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Street Voice API")
                        .version("1.0")
                        .description(
                                "Backend API documentation for Food Street Location-based Audio Guide App. \n\n**Note for Frontend:** Use the `/api/v1/stalls` endpoint to fetch offline data."));
    }
}
