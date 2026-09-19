package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.consumo.ConsumoMaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConcluirOrdemProducaoTest {

    private OrdemProducaoRepositoryEmMemoria ordemRepository;
    private ConsumoMaterialRepositoryEmMemoria consumoRepository;
    private LoteRepositoryEmMemoria loteRepository;
    private MaterialRepositoryEmMemoria materialRepository;
    private ConcluirOrdemProducao casoDeUso;

    private static final LocalDate FABRICACAO = LocalDate.of(2026, 9, 19);
    private static final LocalDate VALIDADE = LocalDate.of(2027, 9, 19);

    @BeforeEach
    void setUp() {
        ordemRepository = new OrdemProducaoRepositoryEmMemoria();
        consumoRepository = new ConsumoMaterialRepositoryEmMemoria();
        loteRepository = new LoteRepositoryEmMemoria();
        materialRepository = new MaterialRepositoryEmMemoria();
        casoDeUso = new ConcluirOrdemProducao(
                ordemRepository, consumoRepository, loteRepository, materialRepository);
    }

    @Test
    @DisplayName("conclui ordem com consumos registrados e gera lote DISPONIVEL")
    void concluiOrdemEGeraLote() {
        OrdemProducao ordem = criarOrdemEmProducao();
        Material material = criarMaterial(ordem.getMaterialId(), "PA-VIGA");
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
        assertThat(lote.getNumeroLote()).startsWith("MAT-PA-VIGA-202609-");
    }

    @Test
    @DisplayName("o número do lote segue o padrão MAT-{codigo}-{yyyyMM}-{seq}")
    void numeroLoteSegueFormato() {
        OrdemProducao ordem = criarOrdemEmProducao();
        criarMaterial(ordem.getMaterialId(), "ACO-1020");
        projetarERegistrarConsumos(ordem.getId(), ordem.getMaterialId());

        ConcluirOrdemProducao.Resultado resultado = casoDeUso.executar(
                new ConcluirOrdemProducao.Comando(
                        ordem.getId(), new BigDecimal("50"), FABRICACAO, VALIDADE));

        assertThat(resultado.lote().getNumeroLote())
                .isEqualTo("MAT-ACO-1020-202609-001");
    }

    @Test
    @DisplayName("rejeita conclusão quando há consumo não registrado")
    void rejeitaConsumoNaoRegistrado() {
        OrdemProducao ordem = criarOrdemEmProducao();
        criarMaterial(ordem.getMaterialId(), "PA-TESTE");

        // Projeta consumo mas não registra
        ConsumoMaterial consumo = ConsumoMaterial.projetar(
                ordem.getId(), UUID.randomUUID(), new BigDecimal("10"), "kg");
        consumoRepository.salvar(consumo);

        assertThatThrownBy(() -> casoDeUso.executar(
                new ConcluirOrdemProducao.Comando(
                        ordem.getId(), new BigDecimal("100"), FABRICACAO, VALIDADE)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não registrado");
    }

    @Test
    @DisplayName("rejeita conclusão quando consumo tem desvio sem justificativa")
    void rejeitaDesvioSemJustificativa() {
        OrdemProducao ordem = criarOrdemEmProducao();
        criarMaterial(ordem.getMaterialId(), "PA-TESTE2");

        // Consumo com desvio mas sem justificativa (força via reconstituir)
        ConsumoMaterial consumoComDesvio = ConsumoMaterial.projetar(
                ordem.getId(), UUID.randomUUID(), new BigDecimal("10"), "kg");
        // Registra com desvio diretamente no domínio (através do método correto com justificativa)
        consumoComDesvio.registrarConsumo(new BigDecimal("15"), "Justificativa de teste", "Operador");
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
                consumoComDesvio.getCriadoEm());
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
                "CNC", 10, LocalDate.now(), LocalDate.now().plusDays(5));
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
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA);
        ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO);
        ordemRepository.salvar(ordem);
        return ordem;
    }

    private Material criarMaterial(UUID id, String codigo) {
        Material material = Material.criar(codigo, "Material de teste",
                TipoMaterial.PRODUTO_ACABADO, "un");
        // Reconstituir com o ID específico da ordem
        Material materialComId = Material.reconstituir(id, material.getCodigo(),
                material.getDescricao(), material.getTipo(),
                material.getUnidadeDeMedida(), material.getCriadoEm(),
                material.getAtualizadoEm());
        materialRepository.salvar(materialComId);
        return materialComId;
    }

    private void projetarERegistrarConsumos(UUID ordemId, UUID materialId) {
        UUID mpId = UUID.randomUUID();
        ConsumoMaterial consumo = ConsumoMaterial.projetar(
                ordemId, mpId, new BigDecimal("50.00"), "kg");
        consumo.registrarConsumo(new BigDecimal("50.00"), null, null);
        consumoRepository.salvar(consumo);
    }
}
