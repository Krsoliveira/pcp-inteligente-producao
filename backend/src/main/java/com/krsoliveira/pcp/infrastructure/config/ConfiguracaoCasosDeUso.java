package com.krsoliveira.pcp.infrastructure.config;

import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
import com.krsoliveira.pcp.application.lista.AtivarListaTecnica;
import com.krsoliveira.pcp.application.lista.CadastrarListaTecnica;
import com.krsoliveira.pcp.application.lista.ConsultarListaTecnica;
import com.krsoliveira.pcp.application.material.CadastrarMaterial;
import com.krsoliveira.pcp.application.material.ConsultarMateriais;
import com.krsoliveira.pcp.application.ordem.AtualizarStatusOrdemProducao;
import com.krsoliveira.pcp.application.ordem.ConsultarOrdensProducao;
import com.krsoliveira.pcp.application.ordem.CriarOrdemProducao;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
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

    // --- Materiais ---

    @Bean
    CadastrarMaterial cadastrarMaterial(MaterialRepository materialRepository) {
        return new CadastrarMaterial(materialRepository);
    }

    @Bean
    ConsultarMateriais consultarMateriais(MaterialRepository materialRepository) {
        return new ConsultarMateriais(materialRepository);
    }

    // --- Listas Técnicas ---

    @Bean
    CadastrarListaTecnica cadastrarListaTecnica(ListaTecnicaRepository listaTecnicaRepository,
                                                MaterialRepository materialRepository) {
        return new CadastrarListaTecnica(listaTecnicaRepository, materialRepository);
    }

    @Bean
    AtivarListaTecnica ativarListaTecnica(ListaTecnicaRepository listaTecnicaRepository) {
        return new AtivarListaTecnica(listaTecnicaRepository);
    }

    @Bean
    ConsultarListaTecnica consultarListaTecnica(ListaTecnicaRepository listaTecnicaRepository) {
        return new ConsultarListaTecnica(listaTecnicaRepository);
    }

    // --- Autenticação ---

    @Bean
    RegistrarUsuario registrarUsuario(UsuarioRepository usuarioRepository,
                                      CodificadorDeSenha codificadorDeSenha) {
        return new RegistrarUsuario(usuarioRepository, codificadorDeSenha);
    }
}
