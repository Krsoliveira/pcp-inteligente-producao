package com.krsoliveira.pcp.infrastructure.config;

import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
import com.krsoliveira.pcp.application.ordem.AtualizarStatusOrdemProducao;
import com.krsoliveira.pcp.application.ordem.ConsultarOrdensProducao;
import com.krsoliveira.pcp.application.ordem.CriarOrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os casos de uso como beans do Spring.
 *
 * Este é o ÚNICO lugar onde aplicação e infraestrutura se encontram:
 * as classes de caso de uso permanecem livres de anotações de framework,
 * e a "cola" fica isolada aqui.
 */
@Configuration
public class ConfiguracaoCasosDeUso {

    // --- Ordens de produção ---

    @Bean
    CriarOrdemProducao criarOrdemProducao(OrdemProducaoRepository repositorio) {
        return new CriarOrdemProducao(repositorio);
    }

    @Bean
    ConsultarOrdensProducao consultarOrdensProducao(OrdemProducaoRepository repositorio) {
        return new ConsultarOrdensProducao(repositorio);
    }

    @Bean
    AtualizarStatusOrdemProducao atualizarStatusOrdemProducao(OrdemProducaoRepository repositorio) {
        return new AtualizarStatusOrdemProducao(repositorio);
    }

    // --- Autenticação ---

    @Bean
    RegistrarUsuario registrarUsuario(UsuarioRepository usuarioRepository,
                                      CodificadorDeSenha codificadorDeSenha) {
        return new RegistrarUsuario(usuarioRepository, codificadorDeSenha);
    }
}
