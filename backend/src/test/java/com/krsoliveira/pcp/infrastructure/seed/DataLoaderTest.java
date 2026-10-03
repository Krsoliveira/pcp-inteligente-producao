package com.krsoliveira.pcp.infrastructure.seed;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.consumo.ConsumoMaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lista.ListaTecnicaRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lote.AlocacaoLoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.TipoOrdemRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import com.krsoliveira.pcp.domain.lote.AlocacaoLote;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
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
    private final AlocacaoLoteRepositoryEmMemoria alocacoes = new AlocacaoLoteRepositoryEmMemoria();
    private final TrilhaDeAuditoriaEmMemoria trilha = new TrilhaDeAuditoriaEmMemoria();

    private DataLoader loaderEm(LocalDate hoje) {
        Clock relogio = Clock.fixed(hoje.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"));
        return new DataLoader(materiais, tipos, listas, ordens, consumos, lotes, alocacoes, trilha, relogio);
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

            var lotesPorOrdem = lotesDeProducao().stream()
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

            assertThat(lotesDeProducao()).allSatisfy(lote -> {
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

    private List<Lote> lotesDeProducao() {
        return lotes.listarTodos().stream().filter(l -> l.getOrdemProducaoId() != null).toList();
    }

    @Nested
    @DisplayName("genealogia (ADR-0011)")
    class Genealogia {

        @Test
        @DisplayName("todo consumo registrado vem de lotes do mesmo material, somando o consumido")
        void consumosAlocados() {
            loaderEm(DATA_REFERENCIA).run();

            var alocadoPorConsumo = alocacoes.todas().stream().collect(Collectors.groupingBy(
                    AlocacaoLote::getConsumoMaterialId,
                    Collectors.reducing(BigDecimal.ZERO, AlocacaoLote::getQuantidade, BigDecimal::add)));
            List<ConsumoMaterial> registrados = ordens.listarTodas().stream()
                    .flatMap(o -> consumos.listarPorOrdemProducao(o.getId()).stream())
                    .filter(c -> c.getQuantidadeConsumida() != null && c.getQuantidadeConsumida().signum() > 0)
                    .toList();
            assertThat(registrados).isNotEmpty().allSatisfy(c -> assertThat(alocadoPorConsumo.get(c.getId()))
                    .isEqualByComparingTo(c.getQuantidadeConsumida()));
            assertThat(alocacoes.todas()).allSatisfy(a -> {
                Lote lote = lotes.buscarPorId(a.getLoteId()).orElseThrow();
                ConsumoMaterial consumo = consumos.buscarPorId(a.getConsumoMaterialId()).orElseThrow();
                assertThat(lote.getMaterialId()).isEqualTo(consumo.getMaterialId());
                assertThat(a.getCriadoEm()).isAfterOrEqualTo(lote.getCriadoEm());
            });
        }

        @Test
        @DisplayName("saldo = quantidade − alocado; saldo zero ⇔ CONSUMIDO")
        void saldos() {
            loaderEm(DATA_REFERENCIA).run();

            var alocadoPorLote = alocacoes.todas().stream().collect(Collectors.groupingBy(
                    AlocacaoLote::getLoteId,
                    Collectors.reducing(BigDecimal.ZERO, AlocacaoLote::getQuantidade, BigDecimal::add)));
            assertThat(lotes.listarTodos()).allSatisfy(lote -> {
                BigDecimal alocado = alocadoPorLote.getOrDefault(lote.getId(), BigDecimal.ZERO);
                assertThat(lote.getSaldo().add(alocado)).isEqualByComparingTo(lote.getQuantidade());
                assertThat(lote.getSaldo().signum() == 0).isEqualTo(lote.getStatus() == StatusLote.CONSUMIDO);
            });
        }

        @Test
        @DisplayName("lotes de compra têm nota fiscal; matéria-prima rastreia até a NF")
        void lotesDeCompra() {
            loaderEm(DATA_REFERENCIA).run();

            List<Lote> compras = lotes.listarTodos().stream().filter(Lote::ehDeCompra).toList();
            assertThat(compras).isNotEmpty().allSatisfy(l -> {
                assertThat(l.getNotaFiscal()).isNotBlank();
                assertThat(l.getOrigemCompra().dataRecebimento()).isAfterOrEqualTo(l.getOrigemCompra().dataEmissaoNf());
                assertThat(materiais.buscarPorId(l.getMaterialId()).orElseThrow().getTipo())
                        .isEqualTo(TipoMaterial.MATERIA_PRIMA);
            });
            assertThat(trilha.eventos(AcaoAuditoria.ENTRADA_REGISTRADA)).hasSize(compras.size());
            assertThat(trilha.eventos(AcaoAuditoria.LOTE_ALOCADO)).hasSize(alocacoes.todas().size());
        }

        @Test
        @DisplayName("a carteira em produção tem saldo para os consumos pendentes")
        void saldoParaACarteira() {
            loaderEm(DATA_REFERENCIA).run();

            var pendente = ordens.listarTodas().stream()
                    .filter(o -> o.getStatus() == StatusOrdemProducao.EM_PRODUCAO)
                    .flatMap(o -> consumos.listarPorOrdemProducao(o.getId()).stream())
                    .filter(c -> !c.estaRegistrado())
                    .collect(Collectors.groupingBy(ConsumoMaterial::getMaterialId,
                            Collectors.reducing(BigDecimal.ZERO, ConsumoMaterial::getQuantidadePlanejada, BigDecimal::add)));
            assertThat(pendente).isNotEmpty();
            pendente.forEach((materialId, quantidade) -> assertThat(lotes.listarDisponiveisPorMaterial(materialId)
                    .stream().map(Lote::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add))
                    .isGreaterThanOrEqualTo(quantidade));
        }
    }

    @Nested
    @DisplayName("rastreabilidade (ADR-0011)")
    class Rastreabilidade {

        @Test
        @DisplayName("todo registro é assinado pela carga inicial; consumo justificado, pelo responsável")
        void assinaturas() {
            loaderEm(DATA_REFERENCIA).run();

            assertThat(materiais.listarTodos()).allMatch(m ->
                    DataLoader.RESPONSAVEL.equals(m.getAssinatura().criadoPor()));
            assertThat(ordens.listarTodas()).allMatch(o ->
                    DataLoader.RESPONSAVEL.equals(o.getAssinatura().criadoPor()));
            assertThat(lotes.listarTodos()).allMatch(l ->
                    DataLoader.RESPONSAVEL.equals(l.getAssinatura().criadoPor()));
            assertThat(ordens.listarTodas().stream()
                    .flatMap(o -> consumos.listarPorOrdemProducao(o.getId()).stream())
                    .filter(c -> c.getJustificativa() != null))
                    .isNotEmpty()
                    .allSatisfy(c -> {
                        assertThat(c.getJustificadoPor()).isNotBlank();
                        assertThat(c.getAssinatura().alteradoPor()).isEqualTo(c.getJustificadoPor());
                    });
        }

        @Test
        @DisplayName("dados mestres são cadastrados antes da primeira ordem")
        void dadosMestresAntesDasOrdens() {
            loaderEm(DATA_REFERENCIA).run();

            Instant primeiraOrdem = ordens.listarTodas().stream()
                    .map(OrdemProducao::getCriadaEm).min(Comparator.naturalOrder()).orElseThrow();
            assertThat(materiais.listarTodos()).allMatch(m -> m.getCriadoEm().isBefore(primeiraOrdem));
        }

        @Test
        @DisplayName("a trilha tem o histórico de cada ordem, em ordem cronológica e nunca no futuro")
        void historicoDasOrdens() {
            loaderEm(DATA_REFERENCIA).run();
            Instant agora = DATA_REFERENCIA.atStartOfDay(ZoneOffset.UTC).toInstant();

            assertThat(trilha.eventos()).allMatch(e -> !e.getOcorridoEm().isAfter(agora));
            assertThat(trilha.eventos(AcaoAuditoria.CRIADO).stream()
                    .filter(e -> e.getTipoEntidade() == TipoEntidade.ORDEM_PRODUCAO))
                    .hasSize((int) ordens.contarTodas());

            long concluidas = ordens.listarTodas().stream()
                    .filter(o -> o.getStatus() == StatusOrdemProducao.CONCLUIDA).count();
            assertThat(trilha.eventos(AcaoAuditoria.ORDEM_CONCLUIDA)).hasSize((int) concluidas);
            assertThat(trilha.eventos(AcaoAuditoria.LOTE_GERADO)).hasSize(lotesDeProducao().size());

            OrdemProducao concluida = ordens.listarTodas().stream()
                    .filter(o -> o.getStatus() == StatusOrdemProducao.CONCLUIDA).findFirst().orElseThrow();
            List<AcaoAuditoria> historico = trilha.eventos().stream()
                    .filter(e -> e.getEntidadeId().equals(concluida.getId()))
                    .sorted(Comparator.comparing(EventoAuditoria::getOcorridoEm))
                    .map(EventoAuditoria::getAcao)
                    .filter(a -> a != AcaoAuditoria.CONSUMO_REGISTRADO)
                    .toList();
            assertThat(historico).containsExactly(AcaoAuditoria.CRIADO, AcaoAuditoria.STATUS_ALTERADO,
                    AcaoAuditoria.STATUS_ALTERADO, AcaoAuditoria.ORDEM_CONCLUIDA);
        }
    }
}
