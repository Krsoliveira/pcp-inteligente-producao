package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.lote.AlocacaoLoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lote.LoteNaoEncontradoException;
import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.OrigemCompra;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrarConsumoMaterialTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 3);

    private ConsumoMaterialRepositoryEmMemoria consumos;
    private LoteRepositoryEmMemoria lotes;
    private AlocacaoLoteRepositoryEmMemoria alocacoes;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private RegistrarConsumoMaterial casoDeUso;

    private Material aco;
    private OrdemProducao ordem;
    private Lote loteA;
    private Lote loteB;
    private ConsumoMaterial consumo;

    @BeforeEach
    void setUp() {
        consumos = new ConsumoMaterialRepositoryEmMemoria();
        lotes = new LoteRepositoryEmMemoria();
        alocacoes = new AlocacaoLoteRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        var materiais = new MaterialRepositoryEmMemoria();
        var ordens = new OrdemProducaoRepositoryEmMemoria();
        casoDeUso = new RegistrarConsumoMaterial(consumos, ordens, materiais, lotes, alocacoes,
                trilha.execucao(), Clock.fixed(HOJE.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

        aco = Material.criar("MP-ACO", "Aço", TipoMaterial.MATERIA_PRIMA, "kg", USUARIO_TESTE);
        materiais.salvar(aco);
        ordem = OrdemProducao.criar("OP-1", UUID.randomUUID(), UUID.randomUUID(), null, "CNC", 10,
                HOJE, HOJE.plusDays(5), USUARIO_TESTE);
        ordens.salvar(ordem);
        loteA = loteDeCompra(aco, "NF-A", "30", HOJE.plusMonths(6));
        loteB = loteDeCompra(aco, "NF-B", "100", HOJE.plusMonths(12));
        consumo = ConsumoMaterial.projetar(ordem.getId(), aco.getId(), new BigDecimal("50"), "kg", USUARIO_TESTE);
        consumos.salvar(consumo);
    }

    private Lote loteDeCompra(Material material, String nf, String quantidade, LocalDate validade) {
        Lote lote = Lote.receberCompra("L-" + nf, material.getId(),
                new OrigemCompra("Fornecedor", nf, HOJE.minusDays(10), HOJE.minusDays(5)),
                new BigDecimal(quantidade), material.getUnidadeDeMedida(), HOJE.minusDays(20), validade,
                USUARIO_TESTE);
        lotes.salvar(lote);
        return lote;
    }

    private RegistrarConsumoMaterial.Comando comando(String qtd, String justificativa,
                                                     RegistrarConsumoMaterial.Alocacao... lotesUsados) {
        return new RegistrarConsumoMaterial.Comando(consumo.getId(), new BigDecimal(qtd), justificativa,
                List.of(lotesUsados));
    }

    private static RegistrarConsumoMaterial.Alocacao de(Lote lote, String qtd) {
        return new RegistrarConsumoMaterial.Alocacao(lote.getId(), new BigDecimal(qtd));
    }

    @Nested
    @DisplayName("Registro com alocação de lotes")
    class Sucesso {

        @Test
        @DisplayName("sem desvio, de um lote: baixa o saldo e grava a genealogia")
        void umLote() {
            ConsumoMaterial resultado = casoDeUso.executar(comando("50", null, de(loteB, "50")));

            assertThat(resultado.estaRegistrado()).isTrue();
            assertThat(loteB.getSaldo()).isEqualByComparingTo("50");
            assertThat(loteB.getStatus()).isEqualTo(StatusLote.DISPONIVEL);
            assertThat(alocacoes.todas()).singleElement().satisfies(a -> {
                assertThat(a.getConsumoMaterialId()).isEqualTo(consumo.getId());
                assertThat(a.getLoteId()).isEqualTo(loteB.getId());
                assertThat(a.getQuantidade()).isEqualByComparingTo("50");
                assertThat(a.getCriadoPor()).isEqualTo(USUARIO_TESTE);
            });
        }

        @Test
        @DisplayName("com desvio, de dois lotes: o lote zerado vira CONSUMIDO e tudo entra na trilha")
        void doisLotes() {
            casoDeUso.executar(comando("55", "Perda no setup", de(loteA, "30"), de(loteB, "25")));

            assertThat(loteA.getSaldo()).isEqualByComparingTo("0");
            assertThat(loteA.getStatus()).isEqualTo(StatusLote.CONSUMIDO);
            assertThat(loteB.getSaldo()).isEqualByComparingTo("75");
            assertThat(consumo.getJustificadoPor()).isEqualTo(USUARIO_TESTE);

            assertThat(trilha.eventos(AcaoAuditoria.CONSUMO_REGISTRADO)).singleElement().satisfies(e -> {
                assertThat(e.getTipoEntidade()).isEqualTo(TipoEntidade.ORDEM_PRODUCAO);
                assertThat(e.getDetalhes()).containsEntry("lotes", "L-NF-A: 30; L-NF-B: 25")
                        .containsEntry("desvio", "5");
            });
            assertThat(trilha.eventos(AcaoAuditoria.LOTE_ALOCADO)).hasSize(2)
                    .anySatisfy(e -> {
                        assertThat(e.getEntidadeId()).isEqualTo(loteA.getId());
                        assertThat(e.getDetalhes())
                                .containsEntry("saldo", Map.of("de", "30", "para", "0"))
                                .containsEntry("status", Map.of("de", "DISPONIVEL", "para", "CONSUMIDO"))
                                .containsEntry("ordemProducao", "OP-1");
                    });
        }

        @Test
        @DisplayName("consumo zero não aloca lotes")
        void consumoZero() {
            casoDeUso.executar(comando("0", "Material substituído"));

            assertThat(consumo.estaRegistrado()).isTrue();
            assertThat(alocacoes.todas()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Regras de alocação")
    class Regras {

        @Test
        @DisplayName("soma das alocações diferente do consumido é recusada e nada muda")
        void somaDiferente() {
            assertThatThrownBy(() -> casoDeUso.executar(comando("50", null, de(loteB, "40"))))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("soma");
            assertNadaMudou();
        }

        @Test
        @DisplayName("consumo maior que zero exige alocação")
        void semAlocacao() {
            assertThatThrownBy(() -> casoDeUso.executar(comando("50", null)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("de quais lotes");
            assertNadaMudou();
        }

        @Test
        @DisplayName("consumo zero com lote informado é recusado")
        void zeroComLote() {
            assertThatThrownBy(() -> casoDeUso.executar(comando("0", "x", de(loteB, "1"))))
                    .isInstanceOf(RegraDeNegocioException.class);
        }

        @Test
        @DisplayName("mesmo lote duas vezes é recusado")
        void loteRepetido() {
            assertThatThrownBy(() -> casoDeUso.executar(comando("50", null, de(loteB, "25"), de(loteB, "25"))))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("mais de uma vez");
        }

        @Test
        @DisplayName("saldo insuficiente é recusado")
        void saldoInsuficiente() {
            assertThatThrownBy(() -> casoDeUso.executar(comando("50", null, de(loteA, "50"))))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Saldo insuficiente");
        }

        @Test
        @DisplayName("lote de outro material é recusado")
        void outroMaterial() {
            Material aluminio = Material.criar("MP-ALU", "Alumínio", TipoMaterial.MATERIA_PRIMA, "kg", USUARIO_TESTE);
            Lote loteAluminio = loteDeCompra(aluminio, "NF-X", "100", HOJE.plusMonths(6));

            assertThatThrownBy(() -> casoDeUso.executar(comando("50", null, de(loteAluminio, "50"))))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("não é do material");
        }

        @Test
        @DisplayName("lote vencido é recusado")
        void loteVencido() {
            Lote vencido = loteDeCompra(aco, "NF-V", "100", HOJE.minusDays(1));

            assertThatThrownBy(() -> casoDeUso.executar(comando("50", null, de(vencido, "50"))))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("venceu");
        }

        @Test
        @DisplayName("lote bloqueado é recusado")
        void loteBloqueado() {
            loteB.bloquear(USUARIO_TESTE);

            assertThatThrownBy(() -> casoDeUso.executar(comando("50", null, de(loteB, "50"))))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("não está disponível");
        }

        @Test
        @DisplayName("lote inexistente gera não encontrado")
        void loteInexistente() {
            assertThatThrownBy(() -> casoDeUso.executar(new RegistrarConsumoMaterial.Comando(consumo.getId(),
                    new BigDecimal("50"), null, List.of(new RegistrarConsumoMaterial.Alocacao(UUID.randomUUID(),
                    new BigDecimal("50"))))))
                    .isInstanceOf(LoteNaoEncontradoException.class);
        }

        @Test
        @DisplayName("consumo já registrado não pode ser registrado de novo")
        void jaRegistrado() {
            casoDeUso.executar(comando("50", null, de(loteB, "50")));

            assertThatThrownBy(() -> casoDeUso.executar(comando("50", null, de(loteB, "50"))))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("já foi registrado");
            assertThat(loteB.getSaldo()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("desvio sem justificativa é recusado pelo domínio")
        void desvioSemJustificativa() {
            assertThatThrownBy(() -> casoDeUso.executar(comando("60", null, de(loteB, "60"))))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Justificativa");
            assertNadaMudou();
        }

        @Test
        @DisplayName("consumo inexistente gera não encontrado")
        void consumoInexistente() {
            assertThatThrownBy(() -> casoDeUso.executar(new RegistrarConsumoMaterial.Comando(
                    UUID.randomUUID(), new BigDecimal("10"), null, List.of())))
                    .isInstanceOf(ConsumoMaterialNaoEncontradoException.class);
        }

        private void assertNadaMudou() {
            assertThat(consumo.estaRegistrado()).isFalse();
            assertThat(loteA.getSaldo()).isEqualByComparingTo("30");
            assertThat(loteB.getSaldo()).isEqualByComparingTo("100");
            assertThat(alocacoes.todas()).isEmpty();
            assertThat(trilha.eventos()).isEmpty();
        }
    }
}
