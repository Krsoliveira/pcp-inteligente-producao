package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.lote.AlocacaoLoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoNaoEncontradaException;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.TipoOrdemRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.OrigemCompra;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Item adicionado ao consumo de uma ordem em produção, fora da lista técnica (ADR-0025). */
class AdicionarConsumoMaterialTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 3);

    private ConsumoMaterialRepositoryEmMemoria consumos;
    private LoteRepositoryEmMemoria lotes;
    private AlocacaoLoteRepositoryEmMemoria alocacoes;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private OrdemProducaoRepositoryEmMemoria ordens;
    private TipoOrdemRepositoryEmMemoria tipos;
    private AdicionarConsumoMaterial casoDeUso;

    private Material produzido;
    private Material cola;
    private Material acabado;
    private OrdemProducao ordem;
    private Lote loteDaCola;

    @BeforeEach
    void setUp() {
        consumos = new ConsumoMaterialRepositoryEmMemoria();
        lotes = new LoteRepositoryEmMemoria();
        alocacoes = new AlocacaoLoteRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        ordens = new OrdemProducaoRepositoryEmMemoria();
        tipos = new TipoOrdemRepositoryEmMemoria();
        var materiais = new MaterialRepositoryEmMemoria();
        var registrar = new RegistrarConsumoMaterial(consumos, ordens, materiais, lotes, alocacoes, trilha.execucao(),
                Clock.fixed(HOJE.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        casoDeUso = new AdicionarConsumoMaterial(ordens, materiais, consumos, tipos, registrar, trilha.execucao());

        produzido = material("105000009", TipoMaterial.SEMIACABADO, "un", materiais);
        cola = material("110000005", TipoMaterial.MATERIA_PRIMA, "kg", materiais);
        acabado = material("103000003", TipoMaterial.PRODUTO_ACABADO, "un", materiais);
        ordem = ordemEmProducao(null);
        loteDaCola = loteDeCompra(cola, "NF-C", "20");
    }

    private Material material(String codigo, TipoMaterial tipo, String unidade, MaterialRepositoryEmMemoria repo) {
        Material material = Material.criar(codigo, "Material " + codigo, tipo, unidade, USUARIO_TESTE);
        repo.salvar(material);
        return material;
    }

    private OrdemProducao ordemEmProducao(UUID tipoOrdemId) {
        OrdemProducao nova = OrdemProducao.criar("OP-" + UUID.randomUUID().toString().substring(0, 6), produzido.getId(),
                UUID.randomUUID(), tipoOrdemId, "CNC", 10, HOJE, HOJE.plusDays(5), USUARIO_TESTE);
        nova.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO_TESTE);
        nova.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO_TESTE);
        ordens.salvar(nova);
        return nova;
    }

    private Lote loteDeCompra(Material material, String nf, String quantidade) {
        Lote lote = Lote.receberCompra("L-" + nf, material.getId(),
                new OrigemCompra("Fornecedor", nf, HOJE.minusDays(10), HOJE.minusDays(5)), UUID.randomUUID(),
                new BigDecimal(quantidade), material.getUnidadeDeMedida(), HOJE.minusDays(20), HOJE.plusMonths(6),
                USUARIO_TESTE);
        lotes.salvar(lote);
        return lote;
    }

    private AdicionarConsumoMaterial.Comando comando(OrdemProducao deOrdem, Material item, String qtd, String justificativa, Lote lote) {
        return new AdicionarConsumoMaterial.Comando(deOrdem.getId(), item.getId(), new BigDecimal(qtd), justificativa,
                List.of(new RegistrarConsumoMaterial.Alocacao(lote.getId(), new BigDecimal(qtd))));
    }

    @Test
    @DisplayName("adiciona a linha com planejado 0, já registrada, baixa o saldo do lote e grava a genealogia")
    void adicionaERegistra() {
        ConsumoMaterial novo = casoDeUso.executar(comando(ordem, cola, "4", "Faltou na lista", loteDaCola));

        assertThat(novo.isNaoPlanejado()).isTrue();
        assertThat(novo.getQuantidadePlanejada()).isEqualByComparingTo("0");
        assertThat(novo.getQuantidadeConsumida()).isEqualByComparingTo("4");
        assertThat(novo.getDesvio()).isEqualByComparingTo("4");
        assertThat(novo.estaRegistrado()).isTrue();
        assertThat(novo.estaJustificado()).isTrue();
        assertThat(novo.getUnidadeDeMedida()).isEqualTo("kg");
        assertThat(consumos.listarPorOrdemProducao(ordem.getId())).containsExactly(novo);
        assertThat(loteDaCola.getSaldo()).isEqualByComparingTo("16");
        assertThat(alocacoes.todas()).singleElement().satisfies(a -> assertThat(a.getConsumoMaterialId()).isEqualTo(novo.getId()));
    }

    @Test
    @DisplayName("registra o evento CONSUMO_ADICIONADO (e não CONSUMO_REGISTRADO) com a justificativa")
    void registraEventoDeAdicao() {
        casoDeUso.executar(comando(ordem, cola, "4", "Faltou na lista", loteDaCola));

        assertThat(trilha.eventos(AcaoAuditoria.CONSUMO_ADICIONADO)).singleElement().satisfies(evento -> {
            assertThat(evento.getDetalhes()).containsEntry("justificativa", "Faltou na lista");
            assertThat(evento.getDetalhes()).containsEntry("naoPlanejado", true);
        });
        assertThat(trilha.eventos(AcaoAuditoria.CONSUMO_REGISTRADO)).isEmpty();
    }

    @Test
    @DisplayName("sem justificativa é recusado e nada é gravado (nem a linha, nem a baixa do lote)")
    void exigeJustificativa() {
        assertThatThrownBy(() -> casoDeUso.executar(comando(ordem, cola, "4", " ", loteDaCola)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Justificativa");
        assertThat(loteDaCola.getSaldo()).isEqualByComparingTo("20");
        assertThat(alocacoes.todas()).isEmpty();
    }

    @Test
    @DisplayName("só em ordem em produção")
    void soEmProducao() {
        OrdemProducao planejada = OrdemProducao.criar("OP-PLAN", produzido.getId(), UUID.randomUUID(), null, "CNC", 10,
                HOJE, HOJE.plusDays(5), USUARIO_TESTE);
        ordens.salvar(planejada);

        assertThatThrownBy(() -> casoDeUso.executar(comando(planejada, cola, "4", "x", loteDaCola)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("em produção");
    }

    @Test
    @DisplayName("material que já está na ordem não vira outra linha: os lotes entram na linha dele")
    void materialJaNaOrdem() {
        consumos.salvar(ConsumoMaterial.projetar(ordem.getId(), cola.getId(), new BigDecimal("5"), "kg", USUARIO_TESTE));

        assertThatThrownBy(() -> casoDeUso.executar(comando(ordem, cola, "4", "x", loteDaCola)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("já está na ordem");
    }

    @Test
    @DisplayName("a ordem não consome o que ela mesma produz")
    void naoConsomeOQueProduz() {
        Lote loteDoProduzido = loteDeCompra(produzido, "NF-P", "10");

        assertThatThrownBy(() -> casoDeUso.executar(comando(ordem, produzido, "1", "x", loteDoProduzido)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("produz");
    }

    @Test
    @DisplayName("produto acabado: recusado em ordem comum (sem tipo ou de outro tipo) e aceito na Conversão")
    void produtoAcabadoSoNaConversao() {
        Lote loteDoAcabado = loteDeCompra(acabado, "NF-A", "10");
        TipoOrdem normal = tipos.salvar(TipoOrdem.criar("Produção Normal", null, "#1565c0", USUARIO_TESTE));
        TipoOrdem conversao = tipos.salvar(TipoOrdem.reconstituir(UUID.randomUUID(), "Conversão", null, "#6a1b9a", true,
                new Assinatura("sistema", Instant.now(), "sistema", Instant.now())));
        OrdemProducao comum = ordemEmProducao(normal.getId());
        OrdemProducao deConversao = ordemEmProducao(conversao.getId());

        assertThatThrownBy(() -> casoDeUso.executar(comando(ordem, acabado, "2", "x", loteDoAcabado)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Conversão");
        assertThatThrownBy(() -> casoDeUso.executar(comando(comum, acabado, "2", "x", loteDoAcabado)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Conversão");
        assertThat(loteDoAcabado.getSaldo()).isEqualByComparingTo("10");

        ConsumoMaterial novo = casoDeUso.executar(comando(deConversao, acabado, "2", "Conversão de lote", loteDoAcabado));

        assertThat(novo.getMaterialId()).isEqualTo(acabado.getId());
        assertThat(loteDoAcabado.getSaldo()).isEqualByComparingTo("8");
    }

    @Test
    @DisplayName("quantidade zero ou ausente é recusada")
    void quantidadeObrigatoria() {
        assertThatThrownBy(() -> casoDeUso.executar(new AdicionarConsumoMaterial.Comando(ordem.getId(), cola.getId(),
                BigDecimal.ZERO, "x", List.of()))).isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> casoDeUso.executar(new AdicionarConsumoMaterial.Comando(ordem.getId(), cola.getId(),
                null, "x", List.of()))).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("a soma dos lotes tem que bater com a quantidade consumida")
    void somaDosLotes() {
        assertThatThrownBy(() -> casoDeUso.executar(new AdicionarConsumoMaterial.Comando(ordem.getId(), cola.getId(),
                new BigDecimal("4"), "x", List.of(new RegistrarConsumoMaterial.Alocacao(loteDaCola.getId(), new BigDecimal("3"))))))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("soma");
    }

    @Test
    @DisplayName("ordem ou material inexistentes")
    void inexistentes() {
        assertThatThrownBy(() -> casoDeUso.executar(new AdicionarConsumoMaterial.Comando(UUID.randomUUID(), cola.getId(),
                BigDecimal.ONE, "x", List.of()))).isInstanceOf(OrdemProducaoNaoEncontradaException.class);
        assertThatThrownBy(() -> casoDeUso.executar(new AdicionarConsumoMaterial.Comando(ordem.getId(), UUID.randomUUID(),
                BigDecimal.ONE, "x", List.of()))).isInstanceOf(MaterialNaoEncontradoException.class);
    }
}
