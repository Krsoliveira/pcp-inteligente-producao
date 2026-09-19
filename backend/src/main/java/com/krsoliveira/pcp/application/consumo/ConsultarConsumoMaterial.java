package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterialRepository;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: consultar os consumos de materiais de uma ordem de produção.
 */
public class ConsultarConsumoMaterial {

    private final ConsumoMaterialRepository consumoRepository;

    public ConsultarConsumoMaterial(ConsumoMaterialRepository consumoRepository) {
        this.consumoRepository = consumoRepository;
    }

    public List<ConsumoMaterial> listarPorOrdem(UUID ordemProducaoId) {
        return consumoRepository.listarPorOrdemProducao(ordemProducaoId);
    }

    public ConsumoMaterial porId(UUID id) {
        return consumoRepository.buscarPorId(id)
                .orElseThrow(() -> new ConsumoMaterialNaoEncontradoException(id));
    }
}
