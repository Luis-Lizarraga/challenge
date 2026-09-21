package com.lucho.tienda.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lucho.tienda.constant.OpenApiMessageConstants.*;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                        .info(new Info()
                                .title(TITLE)
                                .version(VERSION)
                                .description(DESCRIPTION))
                        .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                        .components(new Components()
                                .addSecuritySchemes(SCHEME_NAME,
                                        new SecurityScheme()
                                                .name(SCHEME_NAME)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme(SCHEME)
                                                .bearerFormat(BEARER_FORMAT)));
        }
}