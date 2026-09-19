package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Caso de uso: registrar o consumo real de um material em uma ordem de produção.
 *
 * O operador informa a quantidade efetivamente consumida.
 * Se houver desvio em relação ao planejado, justificativa e responsável são obrigatórios.
 */
public class RegistrarConsumoMaterial {

    private final ConsumoMaterialRepository consumoRepository;

    public RegistrarConsumoMaterial(ConsumoMaterialRepository consumoRepository) {
        this.consumoRepository = consumoRepository;
    }

    public record Comando(UUID consumoMaterialId,
                          BigDecimal quantidadeConsumida,
                          String justificativa,
                          String justificadoPor) {}

    public ConsumoMaterial executar(Comando comando) {
        ConsumoMaterial consumo = consumoRepository.buscarPorId(comando.consumoMaterialId())
                .orElseThrow(() -> new ConsumoMaterialNaoEncontradoException(
                        comando.consumoMaterialId()));

        consumo.registrarConsumo(
                comando.quantidadeConsumida(),
                comando.justificativa(),
                comando.justificadoPor());

        return consumoRepository.salvar(consumo);
    }
}
