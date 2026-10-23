package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.lote.AlocacaoLoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoNaoEncontradaException;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.TipoOrdemRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.OrigemCompra;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lançamento do consumo de uma ordem de uma vez (ADR-0026). O "tudo ou nada" depende da transação do banco e é provado
 * no teste de API com PostgreSQL real; aqui ficam as regras de cada linha e do conjunto.
 */
class LancarConsumoDaOrdemTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 3);

    private ConsumoMaterialRepositoryEmMemoria consumos;
    private LoteRepositoryEmMemoria lotes;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private OrdemProducaoRepositoryEmMemoria ordens;
    private LancarConsumoDaOrdem casoDeUso;

    private Material produzido;
    private Material aco;
    private Material cola;
    private OrdemProducao ordem;
    private ConsumoMaterial linhaDoAco;
    private Lote loteDoAco;
    private Lote loteDaCola;

    @BeforeEach
    void setUp() {
        consumos = new ConsumoMaterialRepositoryEmMemoria();
        lotes = new LoteRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        ordens = new OrdemProducaoRepositoryEmMemoria();
        var materiais = new MaterialRepositoryEmMemoria();
        var registrar = new RegistrarConsumoMaterial(consumos, ordens, materiais, lotes, new AlocacaoLoteRepositoryEmMemoria(),
                trilha.execucao(), Clock.fixed(HOJE.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        var adicionar = new AdicionarConsumoMaterial(ordens, materiais, consumos, new TipoOrdemRepositoryEmMemoria(), registrar,
                trilha.execucao());
        casoDeUso = new LancarConsumoDaOrdem(ordens, consumos, materiais, registrar, adicionar, trilha.execucao());

        produzido = salvar(materiais, "105000009", TipoMaterial.SEMIACABADO, "un");
        aco = salvar(materiais, "110000001", TipoMaterial.MATERIA_PRIMA, "kg");
        cola = salvar(materiais, "110000005", TipoMaterial.MATERIA_PRIMA, "kg");
        ordem = ordemEmProducao();
        linhaDoAco = consumos.salvar(ConsumoMaterial.projetar(ordem.getId(), aco.getId(), new BigDecimal("25"), "kg", USUARIO_TESTE));
        loteDoAco = loteDeCompra(aco, "NF-A", "100");
        loteDaCola = loteDeCompra(cola, "NF-C", "20");
    }

    private Material salvar(MaterialRepositoryEmMemoria repo, String codigo, TipoMaterial tipo, String unidade) {
        Material material = Material.criar(codigo, "Material " + codigo, tipo, unidade, USUARIO_TESTE);
        repo.salvar(material);
        return material;
    }

    private OrdemProducao ordemEmProducao() {
        OrdemProducao nova = OrdemProducao.criar("OP-" + UUID.randomUUID().toString().substring(0, 6), produzido.getId(),
                UUID.randomUUID(), null, "CNC", 10, HOJE, HOJE.plusDays(5), USUARIO_TESTE);
        nova.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO_TESTE);
        nova.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO_TESTE);
        ordens.salvar(nova);
        return nova;
    }

    private Lote loteDeCompra(Material material, String nf, String quantidade) {
        Lote lote = Lote.receberCompra("L-" + nf, material.getId(),
                new OrigemCompra("Fornecedor", nf, HOJE.minusDays(10), HOJE.minusDays(5)), UUID.randomUUID(),
                new BigDecimal(quantidade), material.getUnidadeDeMedida(), HOJE.minusDays(20), HOJE.plusMonths(6), USUARIO_TESTE);
        lotes.salvar(lote);
        return lote;
    }

    private static List<RegistrarConsumoMaterial.Alocacao> de(Lote lote, String qtd) {
        return List.of(new RegistrarConsumoMaterial.Alocacao(lote.getId(), new BigDecimal(qtd)));
    }

    private LancarConsumoDaOrdem.Linha linhaDoAco(String qtd, String justificativa) {
        return new LancarConsumoDaOrdem.Linha(linhaDoAco.getId(), new BigDecimal(qtd), justificativa, de(loteDoAco, qtd));
    }

    private LancarConsumoDaOrdem.ItemAdicional itemDeCola(String qtd, String justificativa) {
        return new LancarConsumoDaOrdem.ItemAdicional(cola.getId(), new BigDecimal(qtd), justificativa, de(loteDaCola, qtd));
    }

    @Test
    @DisplayName("lança a linha da lista e o item adicionado de uma vez, baixa os lotes e devolve as duas linhas")
    void lancaTudo() {
        List<ConsumoMaterial> lancados = casoDeUso.executar(new LancarConsumoDaOrdem.Comando(ordem.getId(),
                List.of(linhaDoAco("25", null)), List.of(itemDeCola("4", "Faltou na lista"))));

        assertThat(lancados).hasSize(2);
        assertThat(lancados.get(0).getMaterialId()).isEqualTo(aco.getId());
        assertThat(lancados.get(0).estaRegistrado()).isTrue();
        assertThat(lancados.get(1).isNaoPlanejado()).isTrue();
        assertThat(lancados.get(1).getDesvio()).isEqualByComparingTo("4");
        assertThat(loteDoAco.getSaldo()).isEqualByComparingTo("75");
        assertThat(loteDaCola.getSaldo()).isEqualByComparingTo("16");
        assertThat(trilha.eventos(AcaoAuditoria.CONSUMO_REGISTRADO)).hasSize(1);
        assertThat(trilha.eventos(AcaoAuditoria.CONSUMO_ADICIONADO)).hasSize(1);
    }

    @Test
    @DisplayName("pode lançar só linhas da lista, ou só itens adicionados")
    void soUmDosTipos() {
        assertThat(casoDeUso.executar(new LancarConsumoDaOrdem.Comando(ordem.getId(), List.of(linhaDoAco("25", null)), null)))
                .hasSize(1);
        assertThat(casoDeUso.executar(new LancarConsumoDaOrdem.Comando(ordem.getId(), null, List.of(itemDeCola("4", "x")))))
                .hasSize(1);
    }

    @Test
    @DisplayName("sem nenhuma linha ou item é recusado")
    void exigeAoMenosUm() {
        assertThatThrownBy(() -> casoDeUso.executar(new LancarConsumoDaOrdem.Comando(ordem.getId(), List.of(), List.of())))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("ao menos um");
    }

    @Test
    @DisplayName("só em ordem em produção; ordem inexistente é 404")
    void soEmProducao() {
        OrdemProducao planejada = OrdemProducao.criar("OP-PLAN", produzido.getId(), UUID.randomUUID(), null, "CNC", 10,
                HOJE, HOJE.plusDays(5), USUARIO_TESTE);
        ordens.salvar(planejada);

        assertThatThrownBy(() -> casoDeUso.executar(new LancarConsumoDaOrdem.Comando(planejada.getId(),
                List.of(linhaDoAco("25", null)), List.of())))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("em produção");
        assertThatThrownBy(() -> casoDeUso.executar(new LancarConsumoDaOrdem.Comando(UUID.randomUUID(), List.of(), List.of())))
                .isInstanceOf(OrdemProducaoNaoEncontradaException.class);
    }

    @Test
    @DisplayName("a mesma linha informada duas vezes, ou uma linha de outra ordem, é recusada")
    void linhasInvalidas() {
        assertThatThrownBy(() -> casoDeUso.executar(new LancarConsumoDaOrdem.Comando(ordem.getId(),
                List.of(linhaDoAco("10", null), linhaDoAco("15", null)), List.of())))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("mais de uma vez");

        OrdemProducao outra = ordemEmProducao();
        assertThatThrownBy(() -> casoDeUso.executar(new LancarConsumoDaOrdem.Comando(outra.getId(),
                List.of(linhaDoAco("25", null)), List.of())))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("não pertence a esta ordem");
        assertThatThrownBy(() -> casoDeUso.executar(new LancarConsumoDaOrdem.Comando(ordem.getId(),
                List.of(new LancarConsumoDaOrdem.Linha(UUID.randomUUID(), BigDecimal.ONE, null, List.of())), List.of())))
                .isInstanceOf(ConsumoMaterialNaoEncontradoException.class);
    }

    @Test
    @DisplayName("a recusa de uma linha diz de qual material foi (desvio sem justificativa na linha do aço)")
    void erroInformaOMaterial() {
        assertThatThrownBy(() -> casoDeUso.executar(new LancarConsumoDaOrdem.Comando(ordem.getId(),
                List.of(linhaDoAco("26", " ")), List.of())))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Material 110000001")
                .hasMessageContaining("Justificativa obrigatória");
    }

    @Test
    @DisplayName("a recusa de um item adicionado também diz o material (sem justificativa)")
    void erroDoItemInformaOMaterial() {
        assertThatThrownBy(() -> casoDeUso.executar(new LancarConsumoDaOrdem.Comando(ordem.getId(),
                List.of(), List.of(itemDeCola("4", null)))))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Material 110000005");
    }
}
