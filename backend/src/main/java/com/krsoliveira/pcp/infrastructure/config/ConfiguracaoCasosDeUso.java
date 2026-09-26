package com.krsoliveira.pcp.infrastructure.config;

import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
import com.krsoliveira.pcp.application.consumo.ConsultarConsumoMaterial;
import com.krsoliveira.pcp.application.consumo.ProjetarConsumoMaterial;
import com.krsoliveira.pcp.application.consumo.RegistrarConsumoMaterial;
import com.krsoliveira.pcp.application.lista.AtivarListaTecnica;
import com.krsoliveira.pcp.application.lista.CadastrarListaTecnica;
import com.krsoliveira.pcp.application.lista.ConsultarListaTecnica;
import com.krsoliveira.pcp.application.lote.ConsultarLotes;
import com.krsoliveira.pcp.application.lote.RegistrarEntradaMaterial;
import com.krsoliveira.pcp.application.material.CadastrarMaterial;
import com.krsoliveira.pcp.application.material.ConsultarMateriais;
import com.krsoliveira.pcp.application.ordem.AtualizarStatusOrdemProducao;
import com.krsoliveira.pcp.application.ordem.AtualizarTipoOrdem;
import com.krsoliveira.pcp.application.ordem.CadastrarTipoOrdem;
import com.krsoliveira.pcp.application.ordem.ConcluirOrdemProducao;
import com.krsoliveira.pcp.application.ordem.ConsultarOrdensProducao;
import com.krsoliveira.pcp.application.ordem.ConsultarTiposOrdem;
import com.krsoliveira.pcp.application.ordem.CriarOrdemProducao;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;
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

    // --- Consumo de Material ---

    @Bean
    ProjetarConsumoMaterial projetarConsumoMaterial(ConsumoMaterialRepository consumoRepository,
                                                     ListaTecnicaRepository listaTecnicaRepository) {
        return new ProjetarConsumoMaterial(consumoRepository, listaTecnicaRepository);
    }

    @Bean
    RegistrarConsumoMaterial registrarConsumoMaterial(ConsumoMaterialRepository consumoRepository) {
        return new RegistrarConsumoMaterial(consumoRepository);
    }

    @Bean
    ConsultarConsumoMaterial consultarConsumoMaterial(ConsumoMaterialRepository consumoRepository) {
        return new ConsultarConsumoMaterial(consumoRepository);
    }

    // --- Lotes ---

    @Bean
    ConsultarLotes consultarLotes(LoteRepository loteRepository) {
        return new ConsultarLotes(loteRepository);
    }

    @Bean
    RegistrarEntradaMaterial registrarEntradaMaterial(LoteRepository loteRepository,
                                                      MaterialRepository materialRepository) {
        return new RegistrarEntradaMaterial(loteRepository, materialRepository);
    }

    // --- Ordens de produção ---

    @Bean
    CriarOrdemProducao criarOrdemProducao(OrdemProducaoRepository ordemRepository,
                                          ProjetarConsumoMaterial projetarConsumoMaterial) {
        return new CriarOrdemProducao(ordemRepository, projetarConsumoMaterial);
    }

    @Bean
    ConsultarOrdensProducao consultarOrdensProducao(OrdemProducaoRepository repositorio) {
        return new ConsultarOrdensProducao(repositorio);
    }

    @Bean
    AtualizarStatusOrdemProducao atualizarStatusOrdemProducao(OrdemProducaoRepository repositorio) {
        return new AtualizarStatusOrdemProducao(repositorio);
    }

    @Bean
    ConcluirOrdemProducao concluirOrdemProducao(OrdemProducaoRepository ordemRepository,
                                                 ConsumoMaterialRepository consumoRepository,
                                                 LoteRepository loteRepository,
                                                 MaterialRepository materialRepository) {
        return new ConcluirOrdemProducao(ordemRepository, consumoRepository,
                loteRepository, materialRepository);
    }

    // --- Tipos de Ordem ---

    @Bean
    CadastrarTipoOrdem cadastrarTipoOrdem(TipoOrdemRepository tipoOrdemRepository) {
        return new CadastrarTipoOrdem(tipoOrdemRepository);
    }

    @Bean
    ConsultarTiposOrdem consultarTiposOrdem(TipoOrdemRepository tipoOrdemRepository) {
        return new ConsultarTiposOrdem(tipoOrdemRepository);
    }

    @Bean
    AtualizarTipoOrdem atualizarTipoOrdem(TipoOrdemRepository tipoOrdemRepository) {
        return new AtualizarTipoOrdem(tipoOrdemRepository);
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
