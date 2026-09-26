package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrarEntradaMaterialTest {

    private static final LocalDate FABRICACAO = LocalDate.of(2026, 9, 10);
    private static final LocalDate VALIDADE = LocalDate.of(2027, 9, 10);

    private final MaterialRepositoryEmMemoria materiais = new MaterialRepositoryEmMemoria();
    private final LoteRepositoryEmMemoria lotes = new LoteRepositoryEmMemoria();
    private final RegistrarEntradaMaterial casoDeUso = new RegistrarEntradaMaterial(lotes, materiais);

    private Material aco;
    private Material semiacabado;

    @BeforeEach
    void preparar() {
        aco = Material.criar("MP-ACO-1045", "Barra aço SAE 1045", TipoMaterial.MATERIA_PRIMA, "kg");
        semiacabado = Material.criar("SA-EIXO", "Eixo usinado", TipoMaterial.SEMIACABADO, "un");
        materiais.salvar(aco);
        materiais.salvar(semiacabado);
    }

    private RegistrarEntradaMaterial.Comando comando(UUID materialId, String fornecedor, String nf) {
        return new RegistrarEntradaMaterial.Comando(materialId, fornecedor, nf,
                new BigDecimal("500.0000"), FABRICACAO, VALIDADE);
    }

    @Test
    @DisplayName("registra entrada de matéria-prima gerando lote DISPONIVEL rastreável")
    void registraEntrada() {
        Lote lote = casoDeUso.executar(comando(aco.getId(), " Aços Brasil Ltda ", " 12345 "));

        assertThat(lote.getNumeroLote()).isEqualTo("MAT-MP-ACO-1045-202609-001");
        assertThat(lote.getStatus()).isEqualTo(StatusLote.DISPONIVEL);
        assertThat(lote.ehDeCompra()).isTrue();
        assertThat(lote.getOrdemProducaoId()).isNull();
        assertThat(lote.getFornecedor()).isEqualTo("Aços Brasil Ltda");
        assertThat(lote.getNotaFiscal()).isEqualTo("12345");
        assertThat(lote.getUnidadeDeMedida()).isEqualTo("kg");
        assertThat(lotes.listarTodos()).containsExactly(lote);
    }

    @Test
    @DisplayName("segunda entrada no mesmo mês recebe o próximo sequencial")
    void sequencial() {
        casoDeUso.executar(comando(aco.getId(), "Fornecedor A", "100"));
        Lote segundo = casoDeUso.executar(comando(aco.getId(), "Fornecedor A", "101"));

        assertThat(segundo.getNumeroLote()).isEqualTo("MAT-MP-ACO-1045-202609-002");
    }

    @Test
    @DisplayName("rejeita material que não é matéria-prima")
    void somenteMateriaPrima() {
        assertThatThrownBy(() -> casoDeUso.executar(comando(semiacabado.getId(), "Fornecedor", "1")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Somente matéria-prima");
        assertThat(lotes.listarTodos()).isEmpty();
    }

    @Test
    @DisplayName("rejeita a mesma nota fiscal do mesmo fornecedor no mesmo material")
    void duplicidade() {
        casoDeUso.executar(comando(aco.getId(), "Fornecedor A", "555"));

        assertThatThrownBy(() -> casoDeUso.executar(comando(aco.getId(), " Fornecedor A ", "555")))
                .isInstanceOf(EntradaMaterialDuplicadaException.class);
        assertThat(lotes.listarTodos()).hasSize(1);
    }

    @Test
    @DisplayName("fornecedor e nota fiscal são obrigatórios")
    void camposObrigatorios() {
        assertThatThrownBy(() -> casoDeUso.executar(comando(aco.getId(), " ", "1")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("fornecedor");
        assertThatThrownBy(() -> casoDeUso.executar(comando(aco.getId(), "Fornecedor", null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nota fiscal");
    }

    @Test
    @DisplayName("material inexistente gera erro de não encontrado")
    void materialInexistente() {
        assertThatThrownBy(() -> casoDeUso.executar(comando(UUID.randomUUID(), "Fornecedor", "1")))
                .isInstanceOf(MaterialNaoEncontradoException.class);
    }
}
