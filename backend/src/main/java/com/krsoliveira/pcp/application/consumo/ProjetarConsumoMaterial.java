package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.lista.ListaTecnicaNaoEncontradaException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: projetar o consumo de materiais a partir da lista técnica
 * quando uma ordem de produção é criada.
 *
 * Para cada item da lista técnica, cria um {@code ConsumoMaterial} com a
 * quantidade planejada = quantidade_item × quantidade_ordem.
 *
 * Chamado por {@link com.krsoliveira.pcp.application.ordem.CriarOrdemProducao}, dentro da
 * mesma transação e com o mesmo usuário responsável.
 */
public class ProjetarConsumoMaterial {

    private final ConsumoMaterialRepository consumoRepository;
    private final ListaTecnicaRepository listaTecnicaRepository;

    public ProjetarConsumoMaterial(ConsumoMaterialRepository consumoRepository,
                                   ListaTecnicaRepository listaTecnicaRepository) {
        this.consumoRepository = consumoRepository;
        this.listaTecnicaRepository = listaTecnicaRepository;
    }

    public record Comando(UUID ordemProducaoId, UUID listaTecnicaId, int quantidadeOrdem,
                          String usuario) {}

    public List<ConsumoMaterial> executar(Comando comando) {
        ListaTecnica lista = listaTecnicaRepository.buscarPorId(comando.listaTecnicaId())
                .orElseThrow(() -> new ListaTecnicaNaoEncontradaException(comando.listaTecnicaId()));

        // quantidade do item ÷ base da lista × quantidade da ordem (ADR-0027)
        List<ConsumoMaterial> consumos = lista.getItens().stream()
                .map(item -> {
                    BigDecimal necessaria = lista.quantidadeNecessaria(item, comando.quantidadeOrdem());
                    if (necessaria.signum() <= 0) {
                        throw new RegraDeNegocioException(
                                ("Um componente da lista (base %s) arredonda para zero numa ordem de %d: "
                                        + "aumente a quantidade da ordem.")
                                        .formatted(lista.getQuantidadeBase().stripTrailingZeros().toPlainString(),
                                                comando.quantidadeOrdem()));
                    }
                    return ConsumoMaterial.projetar(comando.ordemProducaoId(), item.getMaterialComponenteId(),
                            necessaria, item.getUnidadeDeMedida(), comando.usuario());
                })
                .toList();

        return consumoRepository.salvarTodos(consumos);
    }
}
