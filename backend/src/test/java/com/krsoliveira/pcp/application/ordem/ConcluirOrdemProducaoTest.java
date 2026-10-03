package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.consumo.ConsumoMaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConcluirOrdemProducaoTest {

    private OrdemProducaoRepositoryEmMemoria ordemRepository;
    private ConsumoMaterialRepositoryEmMemoria consumoRepository;
    private LoteRepositoryEmMemoria loteRepository;
    private MaterialRepositoryEmMemoria materialRepository;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private ConcluirOrdemProducao casoDeUso;

    private static final LocalDate FABRICACAO = LocalDate.of(2026, 9, 19);
    private static final LocalDate VALIDADE = LocalDate.of(2027, 9, 19);

    @BeforeEach
    void setUp() {
        ordemRepository = new OrdemProducaoRepositoryEmMemoria();
        consumoRepository = new ConsumoMaterialRepositoryEmMemoria();
        loteRepository = new LoteRepositoryEmMemoria();
        materialRepository = new MaterialRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        casoDeUso = new ConcluirOrdemProducao(
                ordemRepository, consumoRepository, loteRepository, materialRepository, trilha.execucao());
    }

    @Test
    @DisplayName("conclui ordem com consumos registrados e gera lote DISPONIVEL")
    void concluiOrdemEGeraLote() {
        OrdemProducao ordem = criarOrdemEmProducao();
        Material material = criarMaterial(ordem.getMaterialId(), "103000001");
        projetarERegistrarConsumos(ordem.getId(), ordem.getMaterialId());

        ConcluirOrdemProducao.Resultado resultado = casoDeUso.executar(
                new ConcluirOrdemProducao.Comando(
                        ordem.getId(), new BigDecimal("100"), FABRICACAO, VALIDADE));

        assertThat(resultado.ordem().getStatus()).isEqualTo(StatusOrdemProducao.CONCLUIDA);
        assertThat(resultado.ordem().getQuantidadeProduzida())
                .isEqualByComparingTo(new BigDecimal("100"));

        Lote lote = resultado.lote();
        assertThat(lote.getStatus()).isEqualTo(StatusLote.DISPONIVEL);
        assertThat(lote.getMaterialId()).isEqualTo(ordem.getMaterialId());
        assertThat(lote.getOrdemProducaoId()).isEqualTo(ordem.getId());
        assertThat(lote.getNumeroLote()).isEqualTo("2609190001");
    }

    @Test
    @DisplayName("registra ORDEM_CONCLUIDA (status de → para) e LOTE_GERADO com o usuário logado")
    void registraEventosDeConclusao() {
        OrdemProducao ordem = criarOrdemEmProducao();
        criarMaterial(ordem.getMaterialId(), "103000001");
        projetarERegistrarConsumos(ordem.getId(), ordem.getMaterialId());

        ConcluirOrdemProducao.Resultado resultado = casoDeUso.executar(
                new ConcluirOrdemProducao.Comando(
                        ordem.getId(), new BigDecimal("100"), FABRICACAO, VALIDADE));

        assertThat(resultado.ordem().getAssinatura().alteradoPor()).isEqualTo(USUARIO_TESTE);
        assertThat(resultado.lote().getAssinatura().criadoPor()).isEqualTo(USUARIO_TESTE);
        assertThat(trilha.eventos(AcaoAuditoria.ORDEM_CONCLUIDA)).singleElement().satisfies(evento -> {
            assertThat(evento.getEntidadeId()).isEqualTo(ordem.getId());
            assertThat(evento.getDetalhes())
                    .containsEntry("status", Map.of("de", "EM_PRODUCAO", "para", "CONCLUIDA"))
                    .containsEntry("loteGerado", resultado.lote().getNumeroLote());
        });
        assertThat(trilha.eventos(AcaoAuditoria.LOTE_GERADO)).singleElement().satisfies(evento -> {
            assertThat(evento.getTipoEntidade()).isEqualTo(TipoEntidade.LOTE);
            assertThat(evento.getEntidadeId()).isEqualTo(resultado.lote().getId());
            assertThat(evento.getDetalhes()).containsEntry("ordemProducao", ordem.getCodigo());
        });
    }

    @Test
    @DisplayName("o número do lote é AAMMDD da fabricação + sequência do dia, sem o código do material")
    void numeroLoteSegueFormato() {
        OrdemProducao ordem = criarOrdemEmProducao();
        criarMaterial(ordem.getMaterialId(), "103000002");
        projetarERegistrarConsumos(ordem.getId(), ordem.getMaterialId());

        ConcluirOrdemProducao.Resultado resultado = casoDeUso.executar(
                new ConcluirOrdemProducao.Comando(
                        ordem.getId(), new BigDecimal("50"), FABRICACAO, VALIDADE));

        assertThat(resultado.lote().getNumeroLote()).isEqualTo("2609190001");

        // Segundo lote do mesmo dia, de outro material: a sequência é do dia, não do material.
        OrdemProducao outra = criarOrdemEmProducao();
        criarMaterial(outra.getMaterialId(), "103000005");
        projetarERegistrarConsumos(outra.getId(), outra.getMaterialId());
        var segundo = casoDeUso.executar(new ConcluirOrdemProducao.Comando(
                outra.getId(), new BigDecimal("10"), FABRICACAO, VALIDADE));
        assertThat(segundo.lote().getNumeroLote()).isEqualTo("2609190002");
    }

    @Test
    @DisplayName("rejeita conclusão quando há consumo não registrado")
    void rejeitaConsumoNaoRegistrado() {
        OrdemProducao ordem = criarOrdemEmProducao();
        criarMaterial(ordem.getMaterialId(), "103000003");

        // Projeta consumo mas não registra
        ConsumoMaterial consumo = ConsumoMaterial.projetar(
                ordem.getId(), UUID.randomUUID(), new BigDecimal("10"), "kg", USUARIO_TESTE);
        consumoRepository.salvar(consumo);

        assertThatThrownBy(() -> casoDeUso.executar(
                new ConcluirOrdemProducao.Comando(
                        ordem.getId(), new BigDecimal("100"), FABRICACAO, VALIDADE)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não registrado");
        assertThat(trilha.eventos()).isEmpty();
    }

    @Test
    @DisplayName("rejeita conclusão quando consumo tem desvio sem justificativa")
    void rejeitaDesvioSemJustificativa() {
        OrdemProducao ordem = criarOrdemEmProducao();
        criarMaterial(ordem.getMaterialId(), "103000004");

        // Consumo com desvio mas sem justificativa (força via reconstituir)
        ConsumoMaterial consumoComDesvio = ConsumoMaterial.projetar(
                ordem.getId(), UUID.randomUUID(), new BigDecimal("10"), "kg", USUARIO_TESTE);
        // Registra com desvio diretamente no domínio (através do método correto com justificativa)
        consumoComDesvio.registrarConsumo(new BigDecimal("15"), "Justificativa de teste", USUARIO_TESTE);
        // Recria sem justificativa via reconstituir para simular estado inválido persistido
        // Na prática, o domínio impede — aqui testamos a validação do use case
        ConsumoMaterial semJustificativa = ConsumoMaterial.reconstituir(
                consumoComDesvio.getId(),
                consumoComDesvio.getOrdemProducaoId(),
                consumoComDesvio.getMaterialId(),
                consumoComDesvio.getQuantidadePlanejada(),
                new BigDecimal("15"), // consumida diferente da planejada
                consumoComDesvio.getUnidadeDeMedida(),
                null, // sem justificativa
                null,
                null,
                consumoComDesvio.getAssinatura());
        consumoRepository.salvar(semJustificativa);

        assertThatThrownBy(() -> casoDeUso.executar(
                new ConcluirOrdemProducao.Comando(
                        ordem.getId(), new BigDecimal("100"), FABRICACAO, VALIDADE)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("desvio");
    }

    @Test
    @DisplayName("rejeita conclusão de ordem que não está EM_PRODUCAO")
    void rejeitaOrdemForaDeEmProducao() {
        OrdemProducao ordem = OrdemProducao.criar(
                "OP-X01", UUID.randomUUID(), UUID.randomUUID(), null,
                "CNC", 10, LocalDate.now(), LocalDate.now().plusDays(5), USUARIO_TESTE);
        ordemRepository.salvar(ordem);

        assertThatThrownBy(() -> casoDeUso.executar(
                new ConcluirOrdemProducao.Comando(
                        ordem.getId(), new BigDecimal("10"), FABRICACAO, VALIDADE)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("EM_PRODUCAO");
    }

    // ---- helpers ----

    private OrdemProducao criarOrdemEmProducao() {
        OrdemProducao ordem = OrdemProducao.criar(
                "OP-" + UUID.randomUUID().toString().substring(0, 6),
                UUID.randomUUID(), UUID.randomUUID(), null,
                "Usinagem CNC", 100,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), USUARIO_TESTE);
        ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO_TESTE);
        ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO_TESTE);
        ordemRepository.salvar(ordem);
        return ordem;
    }

    private Material criarMaterial(UUID id, String codigo) {
        Material material = Material.criar(codigo, "Material de teste",
                TipoMaterial.PRODUTO_ACABADO, "un", USUARIO_TESTE);
        // Reconstituir com o ID específico da ordem
        Material materialComId = Material.reconstituir(id, material.getCodigo(),
                material.getDescricao(), material.getTipo(),
                material.getUnidadeDeMedida(), material.getAssinatura());
        materialRepository.salvar(materialComId);
        return materialComId;
    }

    private void projetarERegistrarConsumos(UUID ordemId, UUID materialId) {
        UUID mpId = UUID.randomUUID();
        ConsumoMaterial consumo = ConsumoMaterial.projetar(
                ordemId, mpId, new BigDecimal("50.00"), "kg", USUARIO_TESTE);
        consumo.registrarConsumo(new BigDecimal("50.00"), null, USUARIO_TESTE);
        consumoRepository.salvar(consumo);
    }
}
