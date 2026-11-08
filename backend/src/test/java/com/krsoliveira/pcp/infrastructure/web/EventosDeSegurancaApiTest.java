package com.krsoliveira.pcp.infrastructure.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.krsoliveira.pcp.BancoDeTesteIsolado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os eventos de segurança chegam ao log (login, login recusado, acesso negado) com o e-mail mascarado e sem senha nem
 * token. O log é lido por um appender temporário do Logback.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "pcp.seguranca.rate-limit.login=1000",
                "pcp.seguranca.rate-limit.registrar=1000"
        })
@BancoDeTesteIsolado
@AutoConfigureTestRestTemplate
@SuppressWarnings({"unchecked", "rawtypes"})
class EventosDeSegurancaApiTest {

    private static final String SENHA = "Senha@123";

    @Autowired
    private TestRestTemplate rest;

    private final ListAppender<ILoggingEvent> registros = new ListAppender<>();
    private final ListAppender<ILoggingEvent> atividade = new ListAppender<>();
    private Logger logDeSeguranca;
    private Logger logDeAtividade;

    @BeforeEach
    void ouvirOLog() {
        logDeSeguranca = (Logger) LoggerFactory.getLogger("pcp.seguranca");
        logDeSeguranca.setLevel(Level.INFO);
        registros.start();
        logDeSeguranca.addAppender(registros);
        logDeAtividade = (Logger) LoggerFactory.getLogger("pcp.atividade");
        logDeAtividade.setLevel(Level.INFO);
        atividade.start();
        logDeAtividade.addAppender(atividade);
    }

    @AfterEach
    void pararDeOuvir() {
        logDeSeguranca.detachAppender(registros);
        registros.stop();
        logDeAtividade.detachAppender(atividade);
        atividade.stop();
    }

    @Test
    @DisplayName("login correto registra login_sucesso com o e-mail mascarado")
    void loginCorreto() {
        cadastrar("ana.logs@teste.local");

        ResponseEntity<Map> resposta = login("ana.logs@teste.local", SENHA);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mensagens()).contains("login_sucesso usuario=a***@teste.local");
        assertThat(mensagens()).noneMatch(m -> m.contains("ana.logs"));
    }

    @Test
    @DisplayName("senha errada registra login_recusado, sem a senha, e a resposta continua igual")
    void senhaErrada() {
        cadastrar("beto.logs@teste.local");

        ResponseEntity<Map> resposta = login("beto.logs@teste.local", "SenhaErrada@999");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(mensagens()).contains("login_recusado usuario=b***@teste.local motivo=credenciais_invalidas");
        assertThat(mensagens()).noneMatch(m -> m.contains("SenhaErrada") || m.contains(SENHA));
    }

    @Test
    @DisplayName("acesso negado registra quem tentou e qual rota, sem o token")
    void acessoNegado() {
        cadastrar("caio.logs@teste.local");
        String token = (String) login("caio.logs@teste.local", SENHA).getBody().get("token");

        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        cabecalhos.setBearerAuth(token);
        ResponseEntity<Map> resposta = rest.postForEntity("/api/v1/acessos/usuarios", new HttpEntity<>(Map.of(
                "nome", "Alguém", "email", "alguem@teste.local", "senhaProvisoria", "Provisoria@1",
                "perfilId", UUID.randomUUID().toString()), cabecalhos), Map.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(mensagens()).contains("acesso_negado usuario=c***@teste.local requisicao=POST /api/v1/acessos/usuarios");
        assertThat(mensagens()).noneMatch(m -> m.contains(token));
    }

    @Test
    @DisplayName("escrita recusada vira uma linha de atividade WARN com o usuário e o status; leitura e login não")
    void atividadeNaCadeiaDeSeguranca() {
        cadastrar("dora.logs@teste.local");
        String token = (String) login("dora.logs@teste.local", SENHA).getBody().get("token");
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        cabecalhos.setBearerAuth(token);

        rest.exchange("/api/v1/acessos/eu", HttpMethod.GET, new HttpEntity<>(cabecalhos), Map.class);
        rest.postForEntity("/api/v1/acessos/usuarios", new HttpEntity<>(Map.of(
                "nome", "Alguém", "email", "alguem@teste.local", "senhaProvisoria", "Provisoria@1",
                "perfilId", UUID.randomUUID().toString()), cabecalhos), Map.class);

        assertThat(atividade.list).singleElement().satisfies(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.WARN);
            assertThat(evento.getFormattedMessage())
                    .startsWith("requisicao usuario=d***@teste.local POST /api/v1/acessos/usuarios status=403 duracaoMs=");
        });
    }

    private List<String> mensagens() {
        return registros.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private void cadastrar(String email) {
        rest.postForEntity("/api/v1/auth/registrar", json(Map.of("nome", "Teste", "email", email, "senha", SENHA)), Void.class);
    }

    private ResponseEntity<Map> login(String email, String senha) {
        return rest.postForEntity("/api/v1/auth/login", json(Map.of("email", email, "senha", senha)), Map.class);
    }

    private static HttpEntity<Map<String, Object>> json(Map<String, Object> corpo) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(corpo, cabecalhos);
    }
}
