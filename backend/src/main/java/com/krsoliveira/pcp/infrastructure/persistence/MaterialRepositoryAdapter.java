package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.CodigoMaterial;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador: implementa a porta {@link MaterialRepository} usando Spring Data JPA.
 */
@Repository
public class MaterialRepositoryAdapter implements MaterialRepository {

    /** Espaço de chaves do bloqueio consultivo da geração de código de material. */
    private static final long CHAVE_BLOQUEIO_CODIGO = 7_300_000L;

    private final MaterialSpringDataRepository springDataRepository;

    public MaterialRepositoryAdapter(MaterialSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public void salvar(Material material) {
        springDataRepository.save(MaterialJpaEntity.deDominio(material));
    }

    @Override
    public Optional<Material> buscarPorId(UUID id) {
        return springDataRepository.findById(id).map(MaterialJpaEntity::paraDominio);
    }

    @Override
    public Optional<Material> buscarPorCodigo(String codigo) {
        return springDataRepository.findByCodigo(codigo).map(MaterialJpaEntity::paraDominio);
    }

    @Override
    public List<Material> listarTodos() {
        return springDataRepository.findAll().stream()
                .map(MaterialJpaEntity::paraDominio)
                .toList();
    }

    /**
     * Bloqueio consultivo do Postgres por tipo, válido até o fim da transação: cadastros
     * simultâneos do mesmo tipo esperam um pelo outro e não colidem no código.
     */
    @Override
    public String proximoCodigo(TipoMaterial tipo) {
        springDataRepository.bloquearFaixaDeCodigo(CHAVE_BLOQUEIO_CODIGO + Integer.parseInt(tipo.prefixoCodigo()));
        return springDataRepository.ultimoCodigoComPrefixo(tipo.prefixoCodigo())
                .map(ultimo -> CodigoMaterial.seguinte(ultimo, tipo))
                .orElseGet(() -> CodigoMaterial.primeiro(tipo));
    }
}
