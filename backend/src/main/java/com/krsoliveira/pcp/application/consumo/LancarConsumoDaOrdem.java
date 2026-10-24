package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoNaoEncontradaException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: lançar de uma vez o consumo de uma ordem em produção (ADR-0026). Registra o consumo de várias linhas
 * da lista técnica e adiciona itens que não estão nela, <b>tudo ou nada</b>: tudo corre na mesma transação, então
 * qualquer recusa desfaz o lançamento inteiro (nenhum saldo de lote é baixado e nenhuma linha é criada).
 *
 * <p>Cada linha segue as mesmas regras do registro individual e do item adicionado ({@link RegistrarConsumoMaterial}
 * e {@link AdicionarConsumoMaterial}). O erro de uma linha informa o material dela.
 */
public class LancarConsumoDaOrdem {

    private final OrdemProducaoRepository ordemRepository;
    private final ConsumoMaterialRepository consumoRepository;
    private final MaterialRepository materialRepository;
    private final RegistrarConsumoMaterial registrarConsumoMaterial;
    private final AdicionarConsumoMaterial adicionarConsumoMaterial;
    private final ExecucaoAuditada execucao;

    public LancarConsumoDaOrdem(OrdemProducaoRepository ordemRepository,
                                ConsumoMaterialRepository consumoRepository,
                                MaterialRepository materialRepository,
                                RegistrarConsumoMaterial registrarConsumoMaterial,
                                AdicionarConsumoMaterial adicionarConsumoMaterial,
                                ExecucaoAuditada execucao) {
        this.ordemRepository = ordemRepository;
        this.consumoRepository = consumoRepository;
        this.materialRepository = materialRepository;
        this.registrarConsumoMaterial = registrarConsumoMaterial;
        this.adicionarConsumoMaterial = adicionarConsumoMaterial;
        this.execucao = execucao;
    }

    /** Consumo de uma linha que já está na ordem (projetada da lista técnica). */
    public record Linha(UUID consumoId, BigDecimal quantidadeConsumida, String justificativa,
                        List<RegistrarConsumoMaterial.Alocacao> alocacoes) {}

    /** Item que não está na lista técnica. */
    public record ItemAdicional(UUID materialId, BigDecimal quantidadeConsumida, String justificativa,
                                List<RegistrarConsumoMaterial.Alocacao> alocacoes) {}

    public record Comando(UUID ordemProducaoId, List<Linha> consumos, List<ItemAdicional> itensAdicionais) {

        public Comando {
            consumos = consumos == null ? List.of() : List.copyOf(consumos);
            itensAdicionais = itensAdicionais == null ? List.of() : List.copyOf(itensAdicionais);
        }
    }

    /** @return as linhas lançadas: primeiro as da lista, depois os itens adicionados, na ordem informada */
    public List<ConsumoMaterial> executar(Comando comando) {
        return execucao.executar(ctx -> {
            OrdemProducao ordem = ordemRepository.buscarPorId(comando.ordemProducaoId())
                    .orElseThrow(() -> new OrdemProducaoNaoEncontradaException(comando.ordemProducaoId()));
            if (ordem.getStatus() != StatusOrdemProducao.EM_PRODUCAO) {
                throw new RegraDeNegocioException("Só é possível lançar o consumo de uma ordem em produção.");
            }
            if (comando.consumos().isEmpty() && comando.itensAdicionais().isEmpty()) {
                throw new RegraDeNegocioException("Informe ao menos um consumo para lançar.");
            }

            Set<UUID> informadas = new HashSet<>();
            for (Linha linha : comando.consumos()) {
                if (linha.consumoId() == null || !informadas.add(linha.consumoId())) {
                    throw new RegraDeNegocioException("A mesma linha de consumo foi informada mais de uma vez.");
                }
            }

            List<ConsumoMaterial> lancados = new ArrayList<>();
            for (Linha linha : comando.consumos()) {
                ConsumoMaterial consumo = consumoRepository.buscarPorId(linha.consumoId())
                        .orElseThrow(() -> new ConsumoMaterialNaoEncontradoException(linha.consumoId()));
                if (!consumo.getOrdemProducaoId().equals(ordem.getId())) {
                    throw new RegraDeNegocioException("A linha de consumo informada não pertence a esta ordem.");
                }
                lancados.add(daLinha(consumo.getMaterialId(), () -> registrarConsumoMaterial.registrarNoContexto(ctx, consumo,
                        new RegistrarConsumoMaterial.Comando(consumo.getId(), linha.quantidadeConsumida(),
                                linha.justificativa(), linha.alocacoes()),
                        AcaoAuditoria.CONSUMO_REGISTRADO)));
            }
            for (ItemAdicional item : comando.itensAdicionais()) {
                lancados.add(daLinha(item.materialId(), () -> adicionarConsumoMaterial.adicionarNoContexto(ctx,
                        new AdicionarConsumoMaterial.Comando(ordem.getId(), item.materialId(), item.quantidadeConsumida(),
                                item.justificativa(), item.alocacoes()))));
            }
            return List.copyOf(lancados);
        });
    }

    /** Executa a linha; se uma regra de negócio recusar, a mensagem diz de qual material foi. */
    private ConsumoMaterial daLinha(UUID materialId, java.util.function.Supplier<ConsumoMaterial> operacao) {
        try {
            return operacao.get();
        } catch (RegraDeNegocioException e) {
            String codigo = materialId == null ? null : materialRepository.buscarPorId(materialId)
                    .map(Material::getCodigo).orElse(null);
            throw codigo == null ? e : new RegraDeNegocioException("Material %s: %s".formatted(codigo, e.getMessage()));
        }
    }
}
