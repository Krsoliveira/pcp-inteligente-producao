package com.krsoliveira.pcp.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O perfil {@code prod} não pode herdar os valores de desenvolvimento de {@code application.yml}: sem a variável, a
 * aplicação não sobe, em vez de assinar tokens com um segredo que está no repositório.
 */
class ConfiguracaoDeProducaoTest {

    @ParameterizedTest(name = "{0} exige {1}")
    @CsvSource({
            "app.jwt.secret, JWT_SECRET",
            "spring.datasource.password, DB_PASSWORD",
    })
    @DisplayName("sem a variável de ambiente, o valor não resolve e a aplicação não sobe")
    void exigeVariavel(String propriedade, String variavel) throws IOException {
        assertThatThrownBy(() -> resolvedorDeProducao(Map.of()).getProperty(propriedade))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(variavel);
    }

    @ParameterizedTest(name = "{0} usa {1}")
    @CsvSource({
            "app.jwt.secret, JWT_SECRET",
            "spring.datasource.password, DB_PASSWORD",
    })
    @DisplayName("com a variável de ambiente, usa o valor dela")
    void usaVariavel(String propriedade, String variavel) throws IOException {
        assertThat(resolvedorDeProducao(Map.of(variavel, "valor-do-ambiente")).getProperty(propriedade))
                .isEqualTo("valor-do-ambiente");
    }

    @Test
    @DisplayName("o Swagger continua desligado em produção")
    void swaggerDesligado() throws IOException {
        assertThat(resolvedorDeProducao(Map.of()).getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
    }

    @Test
    @DisplayName("o autocadastro nasce desligado em produção, e só a variável de ambiente o liga")
    void autocadastroDesligado() throws IOException {
        String propriedade = "pcp.seguranca.autocadastro-habilitado";

        assertThat(resolvedorDeProducao(Map.of()).getProperty(propriedade)).isEqualTo("false");
        assertThat(resolvedorDeProducao(Map.of("AUTOCADASTRO_HABILITADO", "true")).getProperty(propriedade)).isEqualTo("true");
    }

    /** Só os dois YAMLs e o ambiente informado, na ordem do Spring: o perfil vence o arquivo base. */
    private static PropertySourcesPropertyResolver resolvedorDeProducao(Map<String, Object> ambiente) throws IOException {
        MutablePropertySources fontes = new MutablePropertySources();
        fontes.addLast(new MapPropertySource("ambiente", ambiente));
        carregar("application-prod.yml").forEach(fontes::addLast);
        carregar("application.yml").forEach(fontes::addLast);
        return new PropertySourcesPropertyResolver(fontes);
    }

    @Test
    @DisplayName("em produção, os logs saem em JSON e nenhuma espera ao banco fica no padrão da biblioteca")
    void logsETimeouts() throws IOException {
        var producao = resolvedorDeProducao(Map.of());

        assertThat(producao.getProperty("logging.structured.format.console")).isEqualTo("ecs");
        assertThat(producao.getProperty("spring.datasource.hikari.connection-timeout")).isEqualTo("5000");
        assertThat(producao.getProperty("spring.jpa.properties.jakarta.persistence.query.timeout")).isEqualTo("30000");
    }

    private static List<PropertySource<?>> carregar(String arquivo) throws IOException {
        return new YamlPropertySourceLoader().load(arquivo, new ClassPathResource(arquivo));
    }
}
