package com.finsight.finsight_ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI finSightOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinSight AI API")
                        .description("Intelligent financial backend system")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("FinSight Team")));
    }
}

