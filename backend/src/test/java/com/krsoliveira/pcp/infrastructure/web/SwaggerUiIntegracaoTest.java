package com.krsoliveira.pcp.infrastructure.web;

import com.krsoliveira.pcp.BancoDeTesteIsolado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante que o Swagger UI (webjar fixado no pom) continua sendo servido em
 * desenvolvimento e que traz o DOMPurify corrigido (>= 3.4.17).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@BancoDeTesteIsolado
@AutoConfigureTestRestTemplate
class SwaggerUiIntegracaoTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("Swagger UI é servido e o bundle traz DOMPurify 3.4.17")
    void swaggerUiServidoComDomPurifyCorrigido() {
        ResponseEntity<String> pagina = rest.getForEntity("/swagger-ui/index.html", String.class);
        ResponseEntity<String> bundle = rest.getForEntity("/swagger-ui/swagger-ui-bundle.js", String.class);

        assertThat(pagina.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bundle.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bundle.getBody() != null && bundle.getBody().contains("DOMPurify.version=\"3.4.17\""))
                .as("bundle com DOMPurify 3.4.17")
                .isTrue();
    }
}
