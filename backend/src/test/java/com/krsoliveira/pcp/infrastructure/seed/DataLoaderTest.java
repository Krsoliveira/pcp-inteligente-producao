package com.krsoliveira.pcp.infrastructure.seed;

import com.krsoliveira.pcp.application.consumo.ConsumoMaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lista.ListaTecnicaRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.TipoOrdemRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Carrega o dataset sintético REAL (classpath:dados/) em repositórios em memória
 * e verifica que ele respeita as invariantes do domínio (ADR-0007 / ADR-0009).
 */
class DataLoaderTest {

    /** Mesma data_referencia de dataset.properties → deslocamento zero. */
    private static final LocalDate DATA_REFERENCIA = LocalDate.of(2026, 9, 30);

    private final MaterialRepositoryEmMemoria materiais = new MaterialRepositoryEmMemoria();
    private final TipoOrdemRepositoryEmMemoria tipos = new TipoOrdemRepositoryEmMemoria();
    private final ListaTecnicaRepositoryEmMemoria listas = new ListaTecnicaRepositoryEmMemoria();
    private final OrdemProducaoRepositoryEmMemoria ordens = new OrdemProducaoRepositoryEmMemoria();
    private final ConsumoMaterialRepositoryEmMemoria consumos = new ConsumoMaterialRepositoryEmMemoria();
    private final LoteRepositoryEmMemoria lotes = new LoteRepositoryEmMemoria();

    private DataLoader loaderEm(LocalDate hoje) {
        Clock relogio = Clock.fixed(hoje.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"));
        return new DataLoader(materiais, tipos, listas, ordens, consumos, lotes, relogio);
    }

    @Test
    @DisplayName("carrega todas as entidades do dataset")
    void carregaDataset() {
        loaderEm(DATA_REFERENCIA).run();

        assertThat(materiais.listarTodos()).hasSize(21);
        assertThat(tipos.listarTodos()).hasSize(4);
        assertThat(ordens.contarTodas()).isGreaterThan(500);
        assertThat(lotes.listarTodos()).isNotEmpty();
    }

    @Test
    @DisplayName("é idempotente — segunda execução não duplica dados")
    void idempotente() {
        DataLoader loader = loaderEm(DATA_REFERENCIA);
        loader.run();
        long ordensAposPrimeira = ordens.contarTodas();
        int lotesAposPrimeira = lotes.listarTodos().size();

        loader.run();

        assertThat(ordens.contarTodas()).isEqualTo(ordensAposPrimeira);
        assertThat(lotes.listarTodos()).hasSize(lotesAposPrimeira);
    }

    @Nested
    @DisplayName("invariantes do domínio no dataset")
    class Invariantes {

        @Test
        @DisplayName("cada ordem usa uma lista técnica do próprio material")
        void listaDoMesmoMaterial() {
            loaderEm(DATA_REFERENCIA).run();

            assertThat(ordens.listarTodas()).allSatisfy(ordem -> {
                ListaTecnica lista = listas.buscarPorId(ordem.getListaTecnicaId()).orElseThrow();
                assertThat(lista.getMaterialId()).isEqualTo(ordem.getMaterialId());
            });
        }

        @Test
        @DisplayName("no máximo uma lista ATIVA por material; matéria-prima não tem lista")
        void versionamentoDeListas() {
            loaderEm(DATA_REFERENCIA).run();

            for (Material material : materiais.listarTodos()) {
                List<ListaTecnica> doMaterial = listas.listarPorMaterial(material.getId());
                assertThat(doMaterial.stream().filter(l -> l.getStatus() == StatusListaTecnica.ATIVA))
                        .hasSizeLessThanOrEqualTo(1);
                if (material.getTipo().name().equals("MATERIA_PRIMA")) {
                    assertThat(doMaterial).isEmpty();
                }
            }
            assertThat(ordens.listarTodas())
                    .as("o histórico deve conter ordens feitas com versão OBSOLETA de BOM")
                    .anyMatch(o -> listas.buscarPorId(o.getListaTecnicaId()).orElseThrow()
                            .getStatus() == StatusListaTecnica.OBSOLETA);
        }

        @Test
        @DisplayName("ordem concluída: consumos registrados e justificados, quantidade produzida e um lote")
        void ordensConcluidas() {
            loaderEm(DATA_REFERENCIA).run();

            var lotesPorOrdem = lotes.listarTodos().stream()
                    .collect(Collectors.groupingBy(Lote::getOrdemProducaoId, Collectors.counting()));

            List<OrdemProducao> concluidas = ordens.listarTodas().stream()
                    .filter(o -> o.getStatus() == StatusOrdemProducao.CONCLUIDA)
                    .toList();
            assertThat(concluidas).isNotEmpty();
            assertThat(concluidas).allSatisfy(ordem -> {
                List<ConsumoMaterial> doOrdem = consumos.listarPorOrdemProducao(ordem.getId());
                assertThat(doOrdem).isNotEmpty().allMatch(ConsumoMaterial::estaRegistrado)
                        .allMatch(ConsumoMaterial::estaJustificado);
                assertThat(ordem.getQuantidadeProduzida()).isPositive();
                assertThat(lotesPorOrdem.get(ordem.getId())).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("apenas ordens concluídas geram lote")
        void loteSomenteDeConcluidas() {
            loaderEm(DATA_REFERENCIA).run();

            assertThat(lotes.listarTodos()).allSatisfy(lote -> {
                OrdemProducao ordem = ordens.buscarPorId(lote.getOrdemProducaoId()).orElseThrow();
                assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.CONCLUIDA);
                assertThat(lote.getMaterialId()).isEqualTo(ordem.getMaterialId());
            });
        }

        @Test
        @DisplayName("números de lote são únicos e seguem MAT-{codigo}-{yyyyMM}-{seq}")
        void numeroDeLote() {
            loaderEm(DATA_REFERENCIA).run();

            List<Lote> todos = lotes.listarTodos();
            assertThat(todos).extracting(Lote::getNumeroLote).doesNotHaveDuplicates();
            assertThat(todos).allSatisfy(lote -> {
                Material material = materiais.buscarPorId(lote.getMaterialId()).orElseThrow();
                assertThat(lote.getNumeroLote())
                        .startsWith(Lote.prefixoNumeroLote(material.getCodigo(), lote.getDataFabricacao()))
                        .matches(".*-\\d{6}-\\d{3}$");
                assertThat(lote.getDataValidade()).isAfterOrEqualTo(lote.getDataFabricacao());
            });
        }

        @Test
        @DisplayName("o cenário atual tem carteira aberta e ordens atrasadas")
        void cenarioAtual() {
            loaderEm(DATA_REFERENCIA).run();

            List<OrdemProducao> todas = ordens.listarTodas();
            assertThat(todas).anyMatch(o -> o.getStatus().estaAberta());
            assertThat(todas).anyMatch(o -> o.estaAtrasada(DATA_REFERENCIA));
        }
    }

    @Test
    @DisplayName("ancora as datas do dataset na data da carga")
    void ancoragemDeDatas() {
        LocalDate hoje = DATA_REFERENCIA.plusDays(100);

        loaderEm(hoje).run();

        LocalDate ultimaFabricacao = lotes.listarTodos().stream()
                .map(Lote::getDataFabricacao)
                .max(LocalDate::compareTo)
                .orElseThrow();
        assertThat(ultimaFabricacao)
                .as("nenhum lote pode ter sido fabricado depois de 'hoje'")
                .isBeforeOrEqualTo(hoje)
                .isAfter(hoje.minusDays(30));
        assertThat(ordens.listarTodas()).anyMatch(o -> o.estaAtrasada(hoje));
    }
}
