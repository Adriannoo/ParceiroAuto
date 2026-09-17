package br.edu.uniamerica.parceiro_auto.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "ParceiroAuto API",
        version = "v1",
        description = "API de empresas, contas bancárias, movimentações financeiras e usuários."
))
public class OpenApiConfig {
}
