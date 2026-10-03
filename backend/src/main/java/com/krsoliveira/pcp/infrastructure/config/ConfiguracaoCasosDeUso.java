package com.krsoliveira.pcp.infrastructure.config;

import com.krsoliveira.pcp.application.auditoria.ConsultarTrilhaDeAuditoria;
import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.comum.Transacao;
import com.krsoliveira.pcp.application.comum.UsuarioAtual;
import com.krsoliveira.pcp.application.consumo.ConsultarConsumoMaterial;
import com.krsoliveira.pcp.application.consumo.ProjetarConsumoMaterial;
import com.krsoliveira.pcp.application.consumo.RegistrarConsumoMaterial;
import com.krsoliveira.pcp.application.lista.AtivarListaTecnica;
import com.krsoliveira.pcp.application.lista.CadastrarListaTecnica;
import com.krsoliveira.pcp.application.lista.ConsultarListaTecnica;
import com.krsoliveira.pcp.application.lote.ConsultarLotes;
import com.krsoliveira.pcp.application.lote.RastrearLote;
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
import com.krsoliveira.pcp.domain.auditoria.TrilhaDeAuditoria;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.lote.AlocacaoLoteRepository;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;
import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

/**
 * Registra os casos de uso como beans do Spring.
 *
 * Este é o ÚNICO lugar onde aplicação e infraestrutura se encontram:
 * as classes de caso de uso permanecem livres de anotações de framework,
 * e a "cola" fica isolada aqui.
 */
@Configuration
public class ConfiguracaoCasosDeUso {

    // --- Auditoria ---

    @Bean
    ExecucaoAuditada execucaoAuditada(UsuarioAtual usuarioAtual, Transacao transacao,
                                      TrilhaDeAuditoria trilha) {
        return new ExecucaoAuditada(usuarioAtual, transacao, trilha);
    }

    @Bean
    ConsultarTrilhaDeAuditoria consultarTrilhaDeAuditoria(TrilhaDeAuditoria trilha) {
        return new ConsultarTrilhaDeAuditoria(trilha);
    }

    // --- Consumo de Material ---

    @Bean
    ProjetarConsumoMaterial projetarConsumoMaterial(ConsumoMaterialRepository consumoRepository,
                                                     ListaTecnicaRepository listaTecnicaRepository) {
        return new ProjetarConsumoMaterial(consumoRepository, listaTecnicaRepository);
    }

    @Bean
    RegistrarConsumoMaterial registrarConsumoMaterial(ConsumoMaterialRepository consumoRepository,
                                                      OrdemProducaoRepository ordemRepository,
                                                      MaterialRepository materialRepository,
                                                      LoteRepository loteRepository,
                                                      AlocacaoLoteRepository alocacaoRepository,
                                                      ExecucaoAuditada execucao) {
        return new RegistrarConsumoMaterial(consumoRepository, ordemRepository, materialRepository,
                loteRepository, alocacaoRepository, execucao, Clock.systemDefaultZone());
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
    RastrearLote rastrearLote(LoteRepository loteRepository, AlocacaoLoteRepository alocacaoRepository,
                              ConsumoMaterialRepository consumoRepository,
                              OrdemProducaoRepository ordemRepository) {
        return new RastrearLote(loteRepository, alocacaoRepository, consumoRepository, ordemRepository);
    }

    @Bean
    RegistrarEntradaMaterial registrarEntradaMaterial(LoteRepository loteRepository,
                                                      MaterialRepository materialRepository,
                                                      ExecucaoAuditada execucao) {
        return new RegistrarEntradaMaterial(loteRepository, materialRepository, execucao);
    }

    // --- Ordens de produção ---

    @Bean
    CriarOrdemProducao criarOrdemProducao(OrdemProducaoRepository ordemRepository,
                                          ProjetarConsumoMaterial projetarConsumoMaterial,
                                          MaterialRepository materialRepository,
                                          ListaTecnicaRepository listaTecnicaRepository,
                                          ExecucaoAuditada execucao) {
        return new CriarOrdemProducao(ordemRepository, projetarConsumoMaterial, materialRepository,
                listaTecnicaRepository, execucao);
    }

    @Bean
    ConsultarOrdensProducao consultarOrdensProducao(OrdemProducaoRepository repositorio) {
        return new ConsultarOrdensProducao(repositorio);
    }

    @Bean
    AtualizarStatusOrdemProducao atualizarStatusOrdemProducao(OrdemProducaoRepository repositorio,
                                                              ExecucaoAuditada execucao) {
        return new AtualizarStatusOrdemProducao(repositorio, execucao);
    }

    @Bean
    ConcluirOrdemProducao concluirOrdemProducao(OrdemProducaoRepository ordemRepository,
                                                 ConsumoMaterialRepository consumoRepository,
                                                 LoteRepository loteRepository,
                                                 MaterialRepository materialRepository,
                                                 ExecucaoAuditada execucao) {
        return new ConcluirOrdemProducao(ordemRepository, consumoRepository,
                loteRepository, materialRepository, execucao);
    }

    // --- Tipos de ordem ---

    @Bean
    CadastrarTipoOrdem cadastrarTipoOrdem(TipoOrdemRepository tipoOrdemRepository,
                                          ExecucaoAuditada execucao) {
        return new CadastrarTipoOrdem(tipoOrdemRepository, execucao);
    }

    @Bean
    ConsultarTiposOrdem consultarTiposOrdem(TipoOrdemRepository tipoOrdemRepository) {
        return new ConsultarTiposOrdem(tipoOrdemRepository);
    }

    @Bean
    AtualizarTipoOrdem atualizarTipoOrdem(TipoOrdemRepository tipoOrdemRepository,
                                          ExecucaoAuditada execucao) {
        return new AtualizarTipoOrdem(tipoOrdemRepository, execucao);
    }

    // --- Materiais ---

    @Bean
    CadastrarMaterial cadastrarMaterial(MaterialRepository materialRepository, ExecucaoAuditada execucao) {
        return new CadastrarMaterial(materialRepository, execucao);
    }

    @Bean
    ConsultarMateriais consultarMateriais(MaterialRepository materialRepository) {
        return new ConsultarMateriais(materialRepository);
    }

    // --- Listas técnicas ---

    @Bean
    CadastrarListaTecnica cadastrarListaTecnica(ListaTecnicaRepository listaTecnicaRepository,
                                                MaterialRepository materialRepository,
                                                ExecucaoAuditada execucao) {
        return new CadastrarListaTecnica(listaTecnicaRepository, materialRepository, execucao);
    }

    @Bean
    AtivarListaTecnica ativarListaTecnica(ListaTecnicaRepository listaTecnicaRepository,
                                          MaterialRepository materialRepository,
                                          ExecucaoAuditada execucao) {
        return new AtivarListaTecnica(listaTecnicaRepository, materialRepository, execucao);
    }

    @Bean
    ConsultarListaTecnica consultarListaTecnica(ListaTecnicaRepository listaTecnicaRepository) {
        return new ConsultarListaTecnica(listaTecnicaRepository);
    }

    // --- Autenticação ---

    @Bean
    RegistrarUsuario registrarUsuario(UsuarioRepository usuarioRepository,
                                      CodificadorDeSenha codificadorDeSenha,
                                      ExecucaoAuditada execucao) {
        return new RegistrarUsuario(usuarioRepository, codificadorDeSenha, execucao);
    }
}
