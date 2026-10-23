package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtivarListaTecnicaTest {

    private static final String USUARIO = TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;

    private MaterialRepositoryEmMemoria materialRepository;
    private ListaTecnicaRepositoryEmMemoria listaTecnicaRepository;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private AtivarListaTecnica useCase;

    private UUID materialId;
    private UUID listaId;
    private UUID componenteId;

    @BeforeEach
    void setUp() {
        materialRepository   = new MaterialRepositoryEmMemoria();
        listaTecnicaRepository = new ListaTecnicaRepositoryEmMemoria();
        trilha   = new TrilhaDeAuditoriaEmMemoria();
        useCase  = new AtivarListaTecnica(listaTecnicaRepository, materialRepository, trilha.execucao());

        // Material acabado (PA-000001) + componente (MP-000001) — reconstruídos para pular validação de código
        materialId   = UUID.randomUUID();
        listaId      = UUID.randomUUID();
        componenteId = UUID.randomUUID();
        Material material = Material.reconstituir(materialId, "PA-000001", "Produto Teste",
                TipoMaterial.PRODUTO_ACABADO, "UN", Assinatura.nova(USUARIO));
        materialRepository.salvar(material);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ListaTecnica criarListaEmRevisao(String versao) {
        ItemListaTecnica item = ItemListaTecnica.criar(componenteId, BigDecimal.ONE, "UN");
        ListaTecnica lista = ListaTecnica.criarVersao(materialId, listaId, versao, List.of(item), USUARIO);
        listaTecnicaRepository.salvar(lista);
        return lista;
    }

    private ListaTecnica criarListaAtiva(String versao) {
        ItemListaTecnica item = ItemListaTecnica.criar(componenteId, BigDecimal.ONE, "UN");
        // Reconstrói diretamente como ATIVA (simula lista já ativada no passado)
        ListaTecnica lista = ListaTecnica.reconstituir(
                UUID.randomUUID(), listaId, materialId, versao, StatusListaTecnica.ATIVA,
                List.of(item), Assinatura.nova(USUARIO));
        listaTecnicaRepository.salvar(lista);
        return lista;
    }

    // -------------------------------------------------------------------------
    // Cenário principal
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ativa lista em revisão e grava evento de auditoria ATIVADA")
    void ativaListaEmRevisao() {
        ListaTecnica lista = criarListaEmRevisao("v1");

        useCase.executar(lista.getId());

        ListaTecnica salva = listaTecnicaRepository.buscarPorId(lista.getId()).orElseThrow();
        assertThat(salva.getStatus()).isEqualTo(StatusListaTecnica.ATIVA);

        var eventos = trilha.eventos(AcaoAuditoria.ATIVADA);
        assertThat(eventos).hasSize(1);
        assertThat(eventos.getFirst().getReferencia()).contains("v1");
    }

    @Test
    @DisplayName("ativar uma nova versão mantém as outras versões ativas (várias em uso, ADR-0019)")
    void mantemVersoesAtivasAnteriores() {
        ListaTecnica listaAntiga = criarListaAtiva("v1");
        ListaTecnica listaNova   = criarListaEmRevisao("v2");

        useCase.executar(listaNova.getId());

        assertThat(listaTecnicaRepository.buscarPorId(listaAntiga.getId())
                .map(ListaTecnica::getStatus).orElseThrow())
                .isEqualTo(StatusListaTecnica.ATIVA);

        assertThat(listaTecnicaRepository.buscarPorId(listaNova.getId())
                .map(ListaTecnica::getStatus).orElseThrow())
                .isEqualTo(StatusListaTecnica.ATIVA);
    }

    @Test
    @DisplayName("ao ativar nova versão registra só ATIVADA; nada é obsoletado por efeito colateral")
    void registraSoAtivada() {
        criarListaAtiva("v1");
        ListaTecnica listaNova = criarListaEmRevisao("v2");

        useCase.executar(listaNova.getId());

        assertThat(trilha.eventos(AcaoAuditoria.OBSOLETADA)).isEmpty();
        assertThat(trilha.eventos(AcaoAuditoria.ATIVADA)).hasSize(1);
    }

    @Test
    @DisplayName("ativar a primeira versão não produz evento OBSOLETADA")
    void semVersaoAtivaAnteriorNaoObsoleta() {
        ListaTecnica lista = criarListaEmRevisao("v1");

        useCase.executar(lista.getId());

        assertThat(trilha.eventos(AcaoAuditoria.OBSOLETADA)).isEmpty();
        assertThat(trilha.eventos(AcaoAuditoria.ATIVADA)).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Regras de negócio
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("rejeita ativação de lista inexistente")
    void rejeitaListaInexistente() {
        assertThatThrownBy(() -> useCase.executar(UUID.randomUUID()))
                .isInstanceOf(ListaTecnicaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("rejeita ativação de lista já ativa")
    void rejeitaAtivacaoDeListaJaAtiva() {
        ListaTecnica listaAtiva = criarListaAtiva("v1");

        assertThatThrownBy(() -> useCase.executar(listaAtiva.getId()))
                .isInstanceOf(com.krsoliveira.pcp.domain.RegraDeNegocioException.class)
                .hasMessageContaining("revisão");
    }
}