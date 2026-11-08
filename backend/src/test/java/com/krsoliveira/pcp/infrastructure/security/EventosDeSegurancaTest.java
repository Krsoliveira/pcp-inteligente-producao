package com.krsoliveira.pcp.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventosDeSegurancaTest {

    @Test
    @DisplayName("mascara o e-mail: sobra a inicial e o domínio")
    void mascaraOEmail() {
        assertThat(EventosDeSeguranca.mascarar("maria.silva@empresa.com.br")).isEqualTo("m***@empresa.com.br");
    }

    @Test
    @DisplayName("sem e-mail reconhecível, não sobra nada do texto digitado")
    void textoSemArrobaNaoVaza() {
        assertThat(EventosDeSeguranca.mascarar("SenhaDigitadaNoCampoErrado")).isEqualTo("***");
        assertThat(EventosDeSeguranca.mascarar("@dominio.com")).isEqualTo("***");
    }

    @Test
    @DisplayName("ausente ou em branco vira anônimo")
    void ausente() {
        assertThat(EventosDeSeguranca.mascarar(null)).isEqualTo("anonimo");
        assertThat(EventosDeSeguranca.mascarar("  ")).isEqualTo("anonimo");
    }

    @Test
    @DisplayName("quebra de linha no e-mail não forja outra linha de log")
    void quebraDeLinhaNaoForjaLog() {
        String mascarado = EventosDeSeguranca.mascarar("a@x.com\r\nlogin_sucesso usuario=admin@pcp.com");

        assertThat(mascarado).doesNotContain("\r", "\n", " ", "@pcp.com");
    }

    @Test
    @DisplayName("domínio enorme é cortado")
    void dominioEnorme() {
        assertThat(EventosDeSeguranca.mascarar("a@" + "d".repeat(500))).hasSizeLessThan(80);
    }
}
