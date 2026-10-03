package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.lote.LoteNaoEncontradoException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.lote.AlocacaoLote;
import com.krsoliveira.pcp.domain.lote.AlocacaoLoteRepository;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: registrar o consumo real de um material numa ordem, informando de quais
 * lotes ele saiu (genealogia — ADR-0011).
 *
 * Regras: a soma das alocações é igual à quantidade consumida (consumo zero não aloca);
 * cada lote é do mesmo material, está disponível, dentro da validade e tem saldo. Os lotes
 * são lidos com bloqueio, então consumos simultâneos não ultrapassam o saldo. O
 * responsável é o usuário logado; consumo, baixas de saldo e eventos são gravados juntos.
 */
public class RegistrarConsumoMaterial {

    private final ConsumoMaterialRepository consumoRepository;
    private final OrdemProducaoRepository ordemRepository;
    private final MaterialRepository materialRepository;
    private final LoteRepository loteRepository;
    private final AlocacaoLoteRepository alocacaoRepository;
    private final ExecucaoAuditada execucao;
    private final Clock relogio;

    public RegistrarConsumoMaterial(ConsumoMaterialRepository consumoRepository,
                                    OrdemProducaoRepository ordemRepository,
                                    MaterialRepository materialRepository,
                                    LoteRepository loteRepository,
                                    AlocacaoLoteRepository alocacaoRepository,
                                    ExecucaoAuditada execucao,
                                    Clock relogio) {
        this.consumoRepository = consumoRepository;
        this.ordemRepository = ordemRepository;
        this.materialRepository = materialRepository;
        this.loteRepository = loteRepository;
        this.alocacaoRepository = alocacaoRepository;
        this.execucao = execucao;
        this.relogio = relogio;
    }

    /** Quanto sai de cada lote. */
    public record Alocacao(UUID loteId, BigDecimal quantidade) {}

    public record Comando(UUID consumoMaterialId,
                          BigDecimal quantidadeConsumida,
                          String justificativa,
                          List<Alocacao> alocacoes) {

        public Comando {
            alocacoes = alocacoes == null ? List.of() : List.copyOf(alocacoes);
        }
    }

    public ConsumoMaterial executar(Comando comando) {
        return execucao.executar(ctx -> {
            ConsumoMaterial consumo = consumoRepository.buscarPorId(comando.consumoMaterialId())
                    .orElseThrow(() -> new ConsumoMaterialNaoEncontradoException(
                            comando.consumoMaterialId()));

            validarAlocacoes(comando);
            consumo.registrarConsumo(comando.quantidadeConsumida(), comando.justificativa(), ctx.usuario());

            String codigoOrdem = ordemRepository.buscarPorId(consumo.getOrdemProducaoId())
                    .map(OrdemProducao::getCodigo).orElse("");
            String codigoMaterial = materialRepository.buscarPorId(consumo.getMaterialId())
                    .map(Material::getCodigo).orElse(null);
            LocalDate hoje = LocalDate.now(relogio);

            List<AlocacaoLote> alocacoes = new ArrayList<>();
            List<String> resumoLotes = new ArrayList<>();
            for (Alocacao pedido : comando.alocacoes()) {
                Lote lote = loteRepository.buscarPorIdParaAtualizar(pedido.loteId())
                        .orElseThrow(() -> new LoteNaoEncontradoException(pedido.loteId()));
                BigDecimal saldoAntes = lote.getSaldo();
                StatusLote statusAntes = lote.getStatus();

                alocacoes.add(lote.alocar(consumo, pedido.quantidade(), hoje, ctx.usuario()));
                loteRepository.salvar(lote);
                resumoLotes.add(lote.getNumeroLote() + ": "
                        + pedido.quantidade().stripTrailingZeros().toPlainString());

                Detalhes detalhesLote = Detalhes.com("ordemProducao", codigoOrdem)
                        .e("material", codigoMaterial)
                        .e("quantidade", pedido.quantidade())
                        .e("unidadeDeMedida", lote.getUnidadeDeMedida())
                        .mudanca("saldo", saldoAntes, lote.getSaldo());
                if (lote.getStatus() != statusAntes) {
                    detalhesLote.mudanca("status", statusAntes, lote.getStatus());
                }
                ctx.registrar(TipoEntidade.LOTE, lote.getId(), lote.getNumeroLote(),
                        AcaoAuditoria.LOTE_ALOCADO, detalhesLote);
            }

            ConsumoMaterial salvo = consumoRepository.salvar(consumo);
            alocacaoRepository.salvarTodas(alocacoes);

            ctx.registrar(TipoEntidade.ORDEM_PRODUCAO, salvo.getOrdemProducaoId(), codigoOrdem,
                    AcaoAuditoria.CONSUMO_REGISTRADO,
                    Detalhes.com("material", codigoMaterial)
                            .e("quantidadePlanejada", salvo.getQuantidadePlanejada())
                            .e("quantidadeConsumida", salvo.getQuantidadeConsumida())
                            .e("desvio", salvo.getDesvio())
                            .e("unidadeDeMedida", salvo.getUnidadeDeMedida())
                            .e("justificativa", salvo.getJustificativa())
                            .e("lotes", resumoLotes.isEmpty() ? null : String.join("; ", resumoLotes)));
            return salvo;
        });
    }

    private static void validarAlocacoes(Comando comando) {
        if (comando.quantidadeConsumida() == null) {
            return; // o domínio recusa a quantidade ausente com a mensagem adequada
        }
        List<Alocacao> alocacoes = comando.alocacoes();
        boolean consumoZero = comando.quantidadeConsumida().signum() == 0;
        if (consumoZero) {
            if (!alocacoes.isEmpty()) {
                throw new RegraDeNegocioException("Consumo zero não aloca lotes.");
            }
            return;
        }
        if (alocacoes.isEmpty()) {
            throw new RegraDeNegocioException(
                    "Informe de quais lotes saiu o material consumido.");
        }
        Set<UUID> lotes = new HashSet<>();
        for (Alocacao a : alocacoes) {
            if (a.loteId() == null || a.quantidade() == null || a.quantidade().signum() <= 0) {
                throw new RegraDeNegocioException("Cada alocação precisa de lote e quantidade maior que zero.");
            }
            if (!lotes.add(a.loteId())) {
                throw new RegraDeNegocioException("O mesmo lote foi informado mais de uma vez.");
            }
        }
        BigDecimal total = alocacoes.stream().map(Alocacao::quantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(comando.quantidadeConsumida()) != 0) {
            throw new RegraDeNegocioException(
                    "A soma das quantidades dos lotes (%s) deve ser igual à quantidade consumida (%s)."
                            .formatted(total.stripTrailingZeros().toPlainString(),
                                    comando.quantidadeConsumida().stripTrailingZeros().toPlainString()));
        }
    }
}
