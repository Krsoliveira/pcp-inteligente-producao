package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: concluir uma ordem de produção.
 *
 * Orquestração:
 * 1. Valida que a ordem está EM_PRODUCAO.
 * 2. Valida que todos os ConsumoMaterial estão registrados.
 * 3. Valida que desvios têm justificativa.
 * 4. Chama {@code OrdemProducao.concluir(quantidadeProduzida)}.
 * 5. Gera número do lote: {@code MAT-{codigo}-{yyyyMM}-{seq:03d}}.
 * 6. Cria e persiste o Lote com status DISPONIVEL.
 * 7. Persiste a ordem concluída e registra os eventos de auditoria — tudo numa transação.
 *
 * Esta classe substitui a transição direta para CONCLUIDA em
 * {@code AtualizarStatusOrdemProducao}, adicionando rastreabilidade.
 */
public class ConcluirOrdemProducao {

    private final OrdemProducaoRepository ordemRepository;
    private final ConsumoMaterialRepository consumoRepository;
    private final LoteRepository loteRepository;
    private final MaterialRepository materialRepository;
    private final ExecucaoAuditada execucao;

    public ConcluirOrdemProducao(OrdemProducaoRepository ordemRepository,
                                  ConsumoMaterialRepository consumoRepository,
                                  LoteRepository loteRepository,
                                  MaterialRepository materialRepository,
                                  ExecucaoAuditada execucao) {
        this.ordemRepository = ordemRepository;
        this.consumoRepository = consumoRepository;
        this.loteRepository = loteRepository;
        this.materialRepository = materialRepository;
        this.execucao = execucao;
    }

    public record Comando(UUID ordemProducaoId,
                          BigDecimal quantidadeProduzida,
                          LocalDate dataFabricacao,
                          LocalDate dataValidade) {}

    public record Resultado(OrdemProducao ordem, Lote lote) {}

    public Resultado executar(Comando comando) {
        return execucao.executar(ctx -> {
            OrdemProducao ordem = ordemRepository.buscarPorId(comando.ordemProducaoId())
                    .orElseThrow(() -> new OrdemProducaoNaoEncontradaException(
                            comando.ordemProducaoId()));

            // Valida o status ANTES de verificar consumos — mensagem de erro mais precisa
            if (!ordem.getStatus().podeSerConcluida()) {
                throw new RegraDeNegocioException(
                        "Apenas ordens EM_PRODUCAO podem ser concluídas. Status atual: %s."
                                .formatted(ordem.getStatus()));
            }

            List<ConsumoMaterial> consumos = consumoRepository
                    .listarPorOrdemProducao(comando.ordemProducaoId());

            validarConsumos(consumos);

            StatusOrdemProducao statusAnterior = ordem.getStatus();
            ordem.concluir(comando.quantidadeProduzida(), ctx.usuario());

            Material material = materialRepository.buscarPorId(ordem.getMaterialId())
                    .orElseThrow(() -> new MaterialNaoEncontradoException(ordem.getMaterialId()));

            String numeroLote = gerarNumeroLote(material.getCodigo(),
                    comando.dataFabricacao(), ordem.getMaterialId());

            Lote lote = Lote.criar(
                    numeroLote,
                    ordem.getMaterialId(),
                    ordem.getId(),
                    comando.quantidadeProduzida(),
                    material.getUnidadeDeMedida(),
                    comando.dataFabricacao(),
                    comando.dataValidade(),
                    ctx.usuario());

            OrdemProducao ordemSalva = ordemRepository.salvar(ordem);
            Lote loteSalvo = loteRepository.salvar(lote);

            ctx.registrar(TipoEntidade.ORDEM_PRODUCAO, ordemSalva.getId(), ordemSalva.getCodigo(),
                    AcaoAuditoria.ORDEM_CONCLUIDA,
                    Detalhes.vazio()
                            .mudanca("status", statusAnterior, ordemSalva.getStatus())
                            .e("quantidadePlanejada", ordemSalva.getQuantidade())
                            .e("quantidadeProduzida", ordemSalva.getQuantidadeProduzida())
                            .e("loteGerado", loteSalvo.getNumeroLote()));
            ctx.registrar(TipoEntidade.LOTE, loteSalvo.getId(), loteSalvo.getNumeroLote(),
                    AcaoAuditoria.LOTE_GERADO,
                    Detalhes.com("numeroLote", loteSalvo.getNumeroLote())
                            .e("material", material.getCodigo())
                            .e("ordemProducao", ordemSalva.getCodigo())
                            .e("quantidade", loteSalvo.getQuantidade())
                            .e("unidadeDeMedida", loteSalvo.getUnidadeDeMedida())
                            .e("dataFabricacao", loteSalvo.getDataFabricacao())
                            .e("dataValidade", loteSalvo.getDataValidade()));
            return new Resultado(ordemSalva, loteSalvo);
        });
    }

    private void validarConsumos(List<ConsumoMaterial> consumos) {
        if (consumos.isEmpty()) {
            throw new RegraDeNegocioException(
                    "A ordem não possui consumos projetados. " +
                    "Verifique se a lista técnica foi associada corretamente.");
        }

        List<ConsumoMaterial> naoRegistrados = consumos.stream()
                .filter(c -> !c.estaRegistrado())
                .toList();

        if (!naoRegistrados.isEmpty()) {
            throw new RegraDeNegocioException(
                    "Há %d consumo(s) de material ainda não registrado(s). "
                    .formatted(naoRegistrados.size()) +
                    "Registre todos os consumos antes de concluir a ordem.");
        }

        List<ConsumoMaterial> naoJustificados = consumos.stream()
                .filter(c -> !c.estaJustificado())
                .toList();

        if (!naoJustificados.isEmpty()) {
            throw new RegraDeNegocioException(
                    "Há %d consumo(s) com desvio sem justificativa. "
                    .formatted(naoJustificados.size()) +
                    "Justifique todos os desvios antes de concluir a ordem.");
        }
    }

    private String gerarNumeroLote(String codigoMaterial, LocalDate dataFabricacao,
                                   UUID materialId) {
        String prefixo = Lote.prefixoNumeroLote(codigoMaterial, dataFabricacao);
        int seq = loteRepository.proximoSequencial(materialId, prefixo);
        return Lote.numeroLote(prefixo, seq);
    }
}
