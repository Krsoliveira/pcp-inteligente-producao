package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.lista.ListaTecnicaNaoEncontradaException;
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

        List<ConsumoMaterial> consumos = lista.getItens().stream()
                .map(item -> ConsumoMaterial.projetar(
                        comando.ordemProducaoId(),
                        item.getMaterialComponenteId(),
                        item.getQuantidadePlanejada()
                                .multiply(BigDecimal.valueOf(comando.quantidadeOrdem())),
                        item.getUnidadeDeMedida(),
                        comando.usuario()))
                .toList();

        return consumoRepository.salvarTodos(consumos);
    }
}
