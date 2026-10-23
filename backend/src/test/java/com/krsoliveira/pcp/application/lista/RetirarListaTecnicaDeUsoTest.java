package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetirarListaTecnicaDeUsoTest {

    private static final String USUARIO = TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;

    private ListaTecnicaRepositoryEmMemoria listaTecnicaRepository;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private RetirarListaTecnicaDeUso useCase;

    private UUID materialId;
    private UUID listaId;

    @BeforeEach
    void setUp() {
        var materialRepository = new MaterialRepositoryEmMemoria();
        listaTecnicaRepository = new ListaTecnicaRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        useCase = new RetirarListaTecnicaDeUso(listaTecnicaRepository, materialRepository, trilha.execucao());

        materialId = UUID.randomUUID();
        listaId = UUID.randomUUID();
        materialRepository.salvar(Material.reconstituir(materialId, "PA-000001", "Produto Teste",
                TipoMaterial.PRODUTO_ACABADO, "UN", Assinatura.nova(USUARIO)));
    }

    private ListaTecnica salvar(String versao, StatusListaTecnica status) {
        var item = ItemListaTecnica.criar(UUID.randomUUID(), BigDecimal.ONE, "UN");
        var lista = ListaTecnica.reconstituir(UUID.randomUUID(), listaId, materialId, versao, status,
                List.of(item), Assinatura.nova(USUARIO));
        listaTecnicaRepository.salvar(lista);
        return lista;
    }

    @Test
    @DisplayName("retira de uso uma versão ativa e registra OBSOLETADA")
    void retiraVersaoAtiva() {
        ListaTecnica v1 = salvar("v1", StatusListaTecnica.ATIVA);

        useCase.executar(v1.getId());

        assertThat(listaTecnicaRepository.buscarPorId(v1.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusListaTecnica.OBSOLETA);
        assertThat(trilha.eventos(AcaoAuditoria.OBSOLETADA)).hasSize(1);
        assertThat(trilha.eventos(AcaoAuditoria.OBSOLETADA).getFirst().getReferencia()).contains("v1");
    }

    @Test
    @DisplayName("retirar uma versão não mexe nas outras versões ativas do material")
    void naoAfetaAsOutras() {
        ListaTecnica v1 = salvar("v1", StatusListaTecnica.ATIVA);
        ListaTecnica v2 = salvar("v2", StatusListaTecnica.ATIVA);

        useCase.executar(v1.getId());

        assertThat(listaTecnicaRepository.buscarPorId(v2.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusListaTecnica.ATIVA);
    }

    @Test
    @DisplayName("rejeita retirar uma versão que não está ativa")
    void rejeitaVersaoQueNaoEstaAtiva() {
        ListaTecnica emRevisao = salvar("v1", StatusListaTecnica.EM_REVISAO);
        ListaTecnica obsoleta = salvar("v0", StatusListaTecnica.OBSOLETA);

        assertThatThrownBy(() -> useCase.executar(emRevisao.getId()))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("ativas");
        assertThatThrownBy(() -> useCase.executar(obsoleta.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(trilha.eventos(AcaoAuditoria.OBSOLETADA)).isEmpty();
    }

    @Test
    @DisplayName("rejeita lista inexistente")
    void rejeitaInexistente() {
        assertThatThrownBy(() -> useCase.executar(UUID.randomUUID()))
                .isInstanceOf(ListaTecnicaNaoEncontradaException.class);
    }
}
