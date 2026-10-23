package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.consumo.ConsumoMaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.consumo.ProjetarConsumoMaterial;
import com.krsoliveira.pcp.application.lista.ListaTecnicaRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes do caso de uso com repositórios em memória — sem Spring, sem banco.
 */
class CriarOrdemProducaoTest {

    private OrdemProducaoRepositoryEmMemoria repositorio;
    private ListaTecnicaRepositoryEmMemoria listaTecnicaRepositorio;
    private ConsumoMaterialRepositoryEmMemoria consumoRepositorio;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private CriarOrdemProducao casoDeUso;

    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private UUID listaTecnicaId;

    @BeforeEach
    void setUp() {
        repositorio = new OrdemProducaoRepositoryEmMemoria();
        listaTecnicaRepositorio = new ListaTecnicaRepositoryEmMemoria();
        consumoRepositorio = new ConsumoMaterialRepositoryEmMemoria();

        ProjetarConsumoMaterial projetar = new ProjetarConsumoMaterial(
                consumoRepositorio, listaTecnicaRepositorio);
        trilha = new TrilhaDeAuditoriaEmMemoria();
        casoDeUso = new CriarOrdemProducao(repositorio, projetar, new MaterialRepositoryEmMemoria(),
                listaTecnicaRepositorio, trilha.execucao());

        // Pré-cadastrar lista técnica com um componente
        ListaTecnica lista = ListaTecnica.criar(MATERIAL_ID, "v1",
                List.of(ItemListaTecnica.criar(UUID.randomUUID(),
                        new BigDecimal("5.00"), "kg")), USUARIO_TESTE);
        lista.ativar(USUARIO_TESTE);
        listaTecnicaRepositorio.salvar(lista);
        listaTecnicaId = lista.getId();
    }

    private CriarOrdemProducao.Comando comandoValido(String codigo) {
        return new CriarOrdemProducao.Comando(codigo, MATERIAL_ID, listaTecnicaId,
                null, "Usinagem CNC", 50, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20));
    }

    @Test
    @DisplayName("cria e persiste uma ordem válida com status PLANEJADA")
    void criaEPersisteOrdem() {
        OrdemProducao ordem = casoDeUso.executar(comandoValido("OP-0001"));

        assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.PLANEJADA);
        assertThat(ordem.getCentroDeTrabalho()).isEqualTo("Usinagem CNC");
        assertThat(repositorio.buscarPorId(ordem.getId())).isPresent();
    }

    @Test
    @DisplayName("assina ordem e consumos com o usuário logado e registra o evento CRIADO")
    void assinaERegistraEvento() {
        OrdemProducao ordem = casoDeUso.executar(comandoValido("OP-0003"));

        assertThat(ordem.getAssinatura().criadoPor()).isEqualTo(USUARIO_TESTE);
        assertThat(consumoRepositorio.listarPorOrdemProducao(ordem.getId()))
                .allMatch(c -> USUARIO_TESTE.equals(c.getAssinatura().criadoPor()));
        assertThat(trilha.eventos()).singleElement().satisfies(evento -> {
            assertThat(evento.getAcao()).isEqualTo(AcaoAuditoria.CRIADO);
            assertThat(evento.getReferencia()).isEqualTo("OP-0003");
            assertThat(evento.getDetalhes()).containsEntry("versaoListaTecnica", "v1");
        });
    }

    @Test
    @DisplayName("projeta consumos automaticamente ao criar a ordem")
    void projetaConsumosAoCriar() {
        OrdemProducao ordem = casoDeUso.executar(comandoValido("OP-0002"));

        // 1 item na lista × 50 unidades = 1 consumo projetado
        assertThat(consumoRepositorio.listarPorOrdemProducao(ordem.getId()))
                .hasSize(1)
                .allMatch(c -> !c.estaRegistrado());
    }

    @Test
    @DisplayName("aceita qualquer versão ativa do material (várias podem estar em uso)")
    void aceitaOutraVersaoAtiva() {
        ListaTecnica v2 = ListaTecnica.criarVersao(MATERIAL_ID,
                listaTecnicaRepositorio.buscarPorId(listaTecnicaId).orElseThrow().getListaId(), "v2",
                List.of(ItemListaTecnica.criar(UUID.randomUUID(), BigDecimal.ONE, "kg")), USUARIO_TESTE);
        v2.ativar(USUARIO_TESTE);
        listaTecnicaRepositorio.salvar(v2);

        OrdemProducao ordem = casoDeUso.executar(new CriarOrdemProducao.Comando("OP-0010", MATERIAL_ID, v2.getId(),
                null, "Usinagem CNC", 5, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20)));

        assertThat(ordem.getListaTecnicaId()).isEqualTo(v2.getId());
    }

    @Test
    @DisplayName("rejeita versão que não está ativa e não cria ordem nem consumos")
    void rejeitaVersaoQueNaoEstaAtiva() {
        ListaTecnica emRevisao = ListaTecnica.criar(UUID.randomUUID(), "v1",
                List.of(ItemListaTecnica.criar(UUID.randomUUID(), BigDecimal.ONE, "kg")), USUARIO_TESTE);
        listaTecnicaRepositorio.salvar(emRevisao);

        assertThatThrownBy(() -> casoDeUso.executar(new CriarOrdemProducao.Comando("OP-0011",
                emRevisao.getMaterialId(), emRevisao.getId(), null, "Usinagem CNC", 5,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20))))
                .isInstanceOf(com.krsoliveira.pcp.domain.RegraDeNegocioException.class)
                .hasMessageContaining("ativas");
        assertThat(repositorio.listarTodas()).isEmpty();
    }

    @Test
    @DisplayName("rejeita versão de outro material")
    void rejeitaVersaoDeOutroMaterial() {
        assertThatThrownBy(() -> casoDeUso.executar(new CriarOrdemProducao.Comando("OP-0012",
                UUID.randomUUID(), listaTecnicaId, null, "Usinagem CNC", 5,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20))))
                .isInstanceOf(com.krsoliveira.pcp.domain.RegraDeNegocioException.class)
                .hasMessageContaining("material");
        assertThat(repositorio.listarTodas()).isEmpty();
    }

    @Test
    @DisplayName("rejeita versão inexistente")
    void rejeitaVersaoInexistente() {
        assertThatThrownBy(() -> casoDeUso.executar(new CriarOrdemProducao.Comando("OP-0013",
                MATERIAL_ID, UUID.randomUUID(), null, "Usinagem CNC", 5,
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 20))))
                .isInstanceOf(com.krsoliveira.pcp.application.lista.ListaTecnicaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("rejeita código duplicado com CodigoJaUtilizadoException")
    void rejeitaCodigoDuplicado() {
        casoDeUso.executar(comandoValido("OP-0001"));

        assertThatThrownBy(() -> casoDeUso.executar(comandoValido("OP-0001")))
                .isInstanceOf(CodigoJaUtilizadoException.class)
                .hasMessageContaining("OP-0001");
    }
}
