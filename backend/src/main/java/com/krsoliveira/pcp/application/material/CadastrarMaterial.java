package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;

import java.util.UUID;

/**
 * Caso de uso: cadastrar um novo material no sistema.
 *
 * Garante unicidade do código antes de persistir.
 */
public class CadastrarMaterial {

    private final MaterialRepository materialRepository;

    public CadastrarMaterial(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public record Comando(String codigo, String descricao, TipoMaterial tipo,
                          String unidadeDeMedida) {}

    public UUID executar(Comando comando) {
        String codigoNormalizado = comando.codigo() == null
                ? null : comando.codigo().trim().toUpperCase();

        if (materialRepository.existePorCodigo(codigoNormalizado)) {
            throw new CodigoMaterialJaUtilizadoException(codigoNormalizado);
        }

        Material material = Material.criar(
                comando.codigo(),
                comando.descricao(),
                comando.tipo(),
                comando.unidadeDeMedida()
        );

        materialRepository.salvar(material);
        return material.getId();
    }
}