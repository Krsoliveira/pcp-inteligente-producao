package com.krsoliveira.pcp.application.notafiscal;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarNotasFiscaisTest {

    private final MaterialRepositoryEmMemoria materiais = new MaterialRepositoryEmMemoria();
    private final LoteRepositoryEmMemoria lotes = new LoteRepositoryEmMemoria();
    private final NotaFiscalEntradaRepositoryEmMemoria notas = new NotaFiscalEntradaRepositoryEmMemoria();
    private final RegistrarEntradaNotaFiscal registrar = new RegistrarEntradaNotaFiscal(notas, lotes, materiais,
            new TrilhaDeAuditoriaEmMemoria().execucao());
    private final ConsultarNotasFiscais consultar = new ConsultarNotasFiscais(notas, lotes);

    private Material aco;

    @BeforeEach
    void preparar() {
        aco = Material.criar("110000001", "Barra aço", TipoMaterial.MATERIA_PRIMA, "kg", USUARIO_TESTE);
        materiais.salvar(aco);
    }

    private UUID registrar(String numero, LocalDate recebimento, String... lotesDoFornecedor) {
        var itens = java.util.Arrays.stream(lotesDoFornecedor)
                .map(l -> new RegistrarEntradaNotaFiscal.Item(aco.getId(), BigDecimal.TEN, l,
                        recebimento.minusDays(5), recebimento.plusYears(1)))
                .toList();
        return registrar.executar(new RegistrarEntradaNotaFiscal.Comando("Fornecedor", numero,
                recebimento.minusDays(1), recebimento, itens)).nota().getId();
    }

    @Test
    @DisplayName("lista as notas do período, mais recentes primeiro, com seus itens")
    void listaPorPeriodo() {
        registrar("1", LocalDate.of(2026, 9, 1), "A1");
        registrar("2", LocalDate.of(2026, 9, 10), "B1", "B2");
        registrar("3", LocalDate.of(2026, 10, 1), "C1");

        var resultado = consultar.listar(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(resultado).extracting(n -> n.nota().getNumero()).containsExactly("2", "1");
        assertThat(resultado.get(0).itens()).hasSize(2);
        assertThat(consultar.listar(null, null)).hasSize(3);
    }

    @Test
    @DisplayName("período invertido é rejeitado")
    void periodoInvertido() {
        assertThatThrownBy(() -> consultar.listar(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("busca a nota por id com os lotes; inexistente gera NotaFiscalNaoEncontradaException")
    void porId() {
        UUID id = registrar("1", LocalDate.of(2026, 9, 1), "A1", "A2");

        assertThat(consultar.porId(id).itens()).extracting(l -> l.getNumeroLote())
                .containsExactlyInAnyOrder("A1", "A2");
        assertThatThrownBy(() -> consultar.porId(UUID.randomUUID()))
                .isInstanceOf(NotaFiscalNaoEncontradaException.class);
    }
}
