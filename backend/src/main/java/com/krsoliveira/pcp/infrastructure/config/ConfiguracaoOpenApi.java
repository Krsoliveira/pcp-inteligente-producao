package com.krsoliveira.pcp.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados exibidos no Swagger UI ({@code /swagger-ui.html}).
 * Inclui o esquema de autenticação Bearer JWT para que o botão
 * "Authorize" apareça e permita testar endpoints protegidos.
 */
@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plataforma Inteligente de PCP e Produção — API")
                        .description("API de planejamento e controle de produção com apoio de IA.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtido em POST /api/v1/auth/login")));
    }
}
