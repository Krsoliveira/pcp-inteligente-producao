package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoNaoEncontradaException;
import com.krsoliveira.pcp.application.ordem.TipoOrdemNaoEncontradoException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.lista.PoliticaDeComponentes;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: adicionar ao consumo de uma ordem em produção um item que NÃO está na lista técnica (ADR-0025).
 *
 * <p>A linha nasce com quantidade planejada zero e já é registrada: o desvio é tudo o que foi consumido, então a
 * justificativa é obrigatória (regra que já vale para qualquer desvio). Produto acabado só entra em ordem do tipo
 * Conversão (ADR-0024). Mais lotes de um material que já está na ordem entram na linha dele, não numa linha nova.
 */
public class AdicionarConsumoMaterial {

    private final OrdemProducaoRepository ordemRepository;
    private final MaterialRepository materialRepository;
    private final ConsumoMaterialRepository consumoRepository;
    private final TipoOrdemRepository tipoOrdemRepository;
    private final RegistrarConsumoMaterial registrarConsumoMaterial;
    private final ExecucaoAuditada execucao;

    public AdicionarConsumoMaterial(OrdemProducaoRepository ordemRepository,
                                    MaterialRepository materialRepository,
                                    ConsumoMaterialRepository consumoRepository,
                                    TipoOrdemRepository tipoOrdemRepository,
                                    RegistrarConsumoMaterial registrarConsumoMaterial,
                                    ExecucaoAuditada execucao) {
        this.ordemRepository = ordemRepository;
        this.materialRepository = materialRepository;
        this.consumoRepository = consumoRepository;
        this.tipoOrdemRepository = tipoOrdemRepository;
        this.registrarConsumoMaterial = registrarConsumoMaterial;
        this.execucao = execucao;
    }

    public record Comando(UUID ordemProducaoId,
                          UUID materialId,
                          BigDecimal quantidadeConsumida,
                          String justificativa,
                          List<RegistrarConsumoMaterial.Alocacao> alocacoes) {}

    public ConsumoMaterial executar(Comando comando) {
        return execucao.executar(ctx -> adicionarNoContexto(ctx, comando));
    }

    /** Mesma operação dentro de uma execução auditada já aberta (o lançamento de vários itens de uma vez). */
    public ConsumoMaterial adicionarNoContexto(ExecucaoAuditada.Contexto ctx, Comando comando) {
        OrdemProducao ordem = ordemRepository.buscarPorId(comando.ordemProducaoId())
                .orElseThrow(() -> new OrdemProducaoNaoEncontradaException(comando.ordemProducaoId()));
        if (ordem.getStatus() != StatusOrdemProducao.EM_PRODUCAO) {
            throw new RegraDeNegocioException("Só é possível adicionar itens ao consumo de uma ordem em produção.");
        }
        Material item = materialRepository.buscarPorId(comando.materialId())
                .orElseThrow(() -> new MaterialNaoEncontradoException(comando.materialId()));
        if (item.getId().equals(ordem.getMaterialId())) {
            throw new RegraDeNegocioException("O material que a ordem produz não pode ser consumido por ela.");
        }
        TipoOrdem tipo = ordem.getTipoOrdemId() == null ? null : tipoOrdemRepository.buscarPorId(ordem.getTipoOrdemId())
                .orElseThrow(() -> new TipoOrdemNaoEncontradoException(ordem.getTipoOrdemId()));
        PoliticaDeComponentes.validarItemAdicionado(item, tipo);

        boolean jaNaOrdem = consumoRepository.listarPorOrdemProducao(ordem.getId()).stream()
                .anyMatch(c -> c.getMaterialId().equals(item.getId()));
        if (jaNaOrdem) {
            throw new RegraDeNegocioException(
                    "O material %s já está na ordem: inclua os outros lotes na linha dele.".formatted(item.getCodigo()));
        }
        if (comando.quantidadeConsumida() == null || comando.quantidadeConsumida().signum() <= 0) {
            throw new RegraDeNegocioException("A quantidade consumida do item adicionado deve ser maior que zero.");
        }

        ConsumoMaterial consumo = ConsumoMaterial.naoPlanejado(ordem.getId(), item.getId(),
                item.getUnidadeDeMedida(), ctx.usuario());
        return registrarConsumoMaterial.registrarNoContexto(ctx, consumo,
                new RegistrarConsumoMaterial.Comando(consumo.getId(), comando.quantidadeConsumida(),
                        comando.justificativa(), comando.alocacoes()),
                AcaoAuditoria.CONSUMO_ADICIONADO);
    }
}
