package com.krsoliveira.pcp.application.notafiscal;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
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

class RegistrarEntradaNotaFiscalTest {

    private static final LocalDate FABRICACAO = LocalDate.of(2026, 9, 10);
    private static final LocalDate VALIDADE = LocalDate.of(2027, 9, 10);
    private static final LocalDate EMISSAO = LocalDate.of(2026, 9, 12);
    private static final LocalDate RECEBIMENTO = LocalDate.of(2026, 9, 15);

    private final MaterialRepositoryEmMemoria materiais = new MaterialRepositoryEmMemoria();
    private final LoteRepositoryEmMemoria lotes = new LoteRepositoryEmMemoria();
    private final NotaFiscalEntradaRepositoryEmMemoria notas = new NotaFiscalEntradaRepositoryEmMemoria();
    private final TrilhaDeAuditoriaEmMemoria trilha = new TrilhaDeAuditoriaEmMemoria();
    private final RegistrarEntradaNotaFiscal casoDeUso =
            new RegistrarEntradaNotaFiscal(notas, lotes, materiais, trilha.execucao());

    private Material aco;
    private Material tinta;
    private Material semiacabado;

    @BeforeEach
    void preparar() {
        aco = Material.criar("110000001", "Barra aço SAE 1045", TipoMaterial.MATERIA_PRIMA, "kg", USUARIO_TESTE);
        tinta = Material.criar("110000002", "Tinta epóxi", TipoMaterial.MATERIA_PRIMA, "l", USUARIO_TESTE);
        semiacabado = Material.criar("105000001", "Eixo usinado", TipoMaterial.SEMIACABADO, "un", USUARIO_TESTE);
        materiais.salvar(aco);
        materiais.salvar(tinta);
        materiais.salvar(semiacabado);
    }

    private RegistrarEntradaNotaFiscal.Item item(UUID materialId, String numeroLote) {
        return new RegistrarEntradaNotaFiscal.Item(materialId, new BigDecimal("500.0000"), numeroLote,
                FABRICACAO, VALIDADE);
    }

    private RegistrarEntradaNotaFiscal.Comando comando(String fornecedor, String numero,
                                                       RegistrarEntradaNotaFiscal.Item... itens) {
        return new RegistrarEntradaNotaFiscal.Comando(fornecedor, numero, EMISSAO, RECEBIMENTO, List.of(itens));
    }

    @Test
    @DisplayName("registra a nota e um lote DISPONIVEL por item, ligado à nota")
    void registraNotaComItens() {
        var resultado = casoDeUso.executar(comando(" Aços Brasil Ltda ", " 12345 ",
                item(aco.getId(), " ab-2609/01 "), item(tinta.getId(), "T-77")));

        var nota = resultado.nota();
        assertThat(nota.getFornecedor()).isEqualTo("Aços Brasil Ltda");
        assertThat(nota.getNumero()).isEqualTo("12345");
        assertThat(notas.listarTodas()).containsExactly(nota);
        assertThat(resultado.lotes()).hasSize(2).allSatisfy(lote -> {
            assertThat(lote.getNotaFiscalId()).isEqualTo(nota.getId());
            assertThat(lote.getStatus()).isEqualTo(StatusLote.DISPONIVEL);
            assertThat(lote.ehDeCompra()).isTrue();
            assertThat(lote.getOrigemCompra().dataRecebimento()).isEqualTo(RECEBIMENTO);
            assertThat(lote.getAssinatura().criadoPor()).isEqualTo(USUARIO_TESTE);
        });
        assertThat(resultado.lotes()).extracting(Lote::getNumeroLote).containsExactly("AB-2609/01", "T-77");
        assertThat(resultado.lotes().get(1).getUnidadeDeMedida()).isEqualTo("l");
    }

    @Test
    @DisplayName("registra a entrada de cada lote e a nota na trilha de auditoria")
    void registraEventos() {
        var resultado = casoDeUso.executar(comando("Aços Brasil Ltda", "12345",
                item(aco.getId(), "L1"), item(tinta.getId(), "L2")));

        assertThat(trilha.eventos()).hasSize(3);
        assertThat(trilha.eventos()).filteredOn(e -> e.getTipoEntidade() == TipoEntidade.LOTE)
                .hasSize(2).allSatisfy(e -> assertThat(e.getAcao()).isEqualTo(AcaoAuditoria.ENTRADA_REGISTRADA));
        assertThat(trilha.eventos()).last().satisfies(e -> {
            assertThat(e.getTipoEntidade()).isEqualTo(TipoEntidade.NOTA_FISCAL);
            assertThat(e.getEntidadeId()).isEqualTo(resultado.nota().getId());
            assertThat(e.getReferencia()).isEqualTo("12345");
            assertThat(e.getDetalhes()).containsEntry("fornecedor", "Aços Brasil Ltda").containsEntry("itens", 2);
            assertThat(e.getDetalhes().get("lotes").toString()).contains("110000001", "lote L1");
        });
    }

    @Test
    @DisplayName("a mesma nota do mesmo fornecedor (sem diferenciar maiúsculas) não entra duas vezes")
    void notaDuplicada() {
        casoDeUso.executar(comando("Aços Brasil Ltda", "12345", item(aco.getId(), "L1")));

        assertThatThrownBy(() -> casoDeUso.executar(comando("AÇOS BRASIL LTDA", "12345", item(aco.getId(), "L2"))))
                .isInstanceOf(NotaFiscalDuplicadaException.class);
    }

    @Test
    @DisplayName("mesmo número de nota de outro fornecedor é aceito")
    void mesmoNumeroOutroFornecedor() {
        casoDeUso.executar(comando("Aços Brasil Ltda", "12345", item(aco.getId(), "L1")));

        var resultado = casoDeUso.executar(comando("Metalúrgica Sul", "12345", item(aco.getId(), "L1")));

        assertThat(resultado.lotes()).hasSize(1);
    }

    @Test
    @DisplayName("o lote do fornecedor não se repete para o mesmo material")
    void loteDuplicadoDoFornecedor() {
        casoDeUso.executar(comando("Aços Brasil Ltda", "12345", item(aco.getId(), "L1")));

        assertThatThrownBy(() -> casoDeUso.executar(comando("Aços Brasil Ltda", "12346", item(aco.getId(), "l1"))))
                .isInstanceOf(LoteDuplicadoException.class)
                .hasMessageContaining("L1");
    }

    @Test
    @DisplayName("o mesmo lote do mesmo material não pode repetir dentro da nota")
    void loteRepetidoNaNota() {
        assertThatThrownBy(() -> casoDeUso.executar(comando("Aços Brasil Ltda", "12345",
                item(aco.getId(), "L1"), item(aco.getId(), "L1"))))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("se repete");
    }

    @Test
    @DisplayName("nota sem itens é rejeitada")
    void semItens() {
        assertThatThrownBy(() -> casoDeUso.executar(comando("Aços Brasil Ltda", "12345")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("pelo menos um item");
        assertThat(trilha.eventos()).isEmpty();
    }

    @Test
    @DisplayName("somente matéria-prima entra por nota fiscal")
    void somenteMateriaPrima() {
        assertThatThrownBy(() -> casoDeUso.executar(comando("Aços Brasil Ltda", "12345",
                item(semiacabado.getId(), "L1"))))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("matéria-prima");
    }

    @Test
    @DisplayName("material inexistente é rejeitado")
    void materialInexistente() {
        assertThatThrownBy(() -> casoDeUso.executar(comando("Aços Brasil Ltda", "12345",
                item(UUID.randomUUID(), "L1"))))
                .isInstanceOf(MaterialNaoEncontradoException.class);
    }

    @Test
    @DisplayName("recebimento anterior à emissão é rejeitado")
    void recebimentoAntesDaEmissao() {
        assertThatThrownBy(() -> casoDeUso.executar(new RegistrarEntradaNotaFiscal.Comando("Aços Brasil Ltda",
                "12345", RECEBIMENTO, EMISSAO, List.of(item(aco.getId(), "L1")))))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("anterior à emissão");
    }
}
