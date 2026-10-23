package com.mediqueue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_KEY_SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediQueue API — Hospital Appointment & Real-Time Queue Platform")
                        .description("Phase 1 Production-Ready Backend: Authentication, Authorization, Patient Profiles, Doctor & Department Directories, and System Administration.")
                        .version("1.0.0")
                        .contact(new Contact().name("MediQueue Engineering Team").email("engineering@mediqueue.com"))
                        .license(new License().name("Proprietary").url("https://mediqueue.com/terms")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_KEY_SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_KEY_SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_KEY_SECURITY_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT Bearer token obtained from /api/v1/auth/login")));
    }
}
