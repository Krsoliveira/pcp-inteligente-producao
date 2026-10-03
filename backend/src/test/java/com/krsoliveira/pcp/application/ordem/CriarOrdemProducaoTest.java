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
    @DisplayName("rejeita código duplicado com CodigoJaUtilizadoException")
    void rejeitaCodigoDuplicado() {
        casoDeUso.executar(comandoValido("OP-0001"));

        assertThatThrownBy(() -> casoDeUso.executar(comandoValido("OP-0001")))
                .isInstanceOf(CodigoJaUtilizadoException.class)
                .hasMessageContaining("OP-0001");
    }
}
