package com.krsoliveira.pcp.infrastructure.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A lista padrão de proxies confiáveis do perfil prod (PROXIES_CONFIAVEIS, ADR-0033). Só o Nginx do frontend chega à
 * API, pela rede privada do Railway, ora por IPv4 (10.x), ora por IPv6 privado (fd..). Endereço público nunca entra:
 * de quem não é proxy confiável, o X-Forwarded-For é texto do cliente e é ignorado.
 */
class ProxiesConfiaveisDoPerfilProdTest {

    private static final String PREFIXO = "${PROXIES_CONFIAVEIS:";
    private static Pattern proxiesConfiaveis;

    @BeforeAll
    static void lerOPadraoDoPerfilProd() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));
        String valor = yaml.getObject().getProperty("server.tomcat.remoteip.internal-proxies");
        assertThat(valor).startsWith(PREFIXO).endsWith("}");
        // O Tomcat compara o endereço inteiro com a expressão (matches), como aqui.
        proxiesConfiaveis = Pattern.compile(valor.substring(PREFIXO.length(), valor.length() - 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "10.131.182.86",                        // Nginx pela rede privada do Railway, IPv4 (medido)
            "fd12:2f96:51e:1:3000:1a6:5983:b656",   // Nginx pela rede privada do Railway, IPv6 (medido)
            "fc00:0:0:0:0:0:0:1",
            "172.20.0.3",                           // rede do Docker Compose (ensaio local)
            "127.0.0.1",
            "0:0:0:0:0:0:0:1"
    })
    @DisplayName("confia nos endereços privados por onde o Nginx chega à API")
    void confiaNoNginx(String endereco) {
        assertThat(proxiesConfiaveis.matcher(endereco).matches()).as(endereco).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "203.0.113.9",                          // IP forjado por um cliente
            "46.151.194.130",                       // borda pública do Railway (medido)
            "168.90.250.169",                       // um cliente real
            "100.64.0.3",                           // faixa interna da borda: quem confia nela é o Nginx, não a API
            "2001:db8:0:0:0:0:0:1",                 // IPv6 público
            "fe80:0:0:0:0:0:0:1"                    // IPv6 de enlace local
    })
    @DisplayName("não confia em endereço público nem na borda: o X-Forwarded-For deles seria do cliente")
    void naoConfiaEmQuemNaoEProxy(String endereco) {
        assertThat(proxiesConfiaveis.matcher(endereco).matches()).as(endereco).isFalse();
    }
}
