package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.CodigoMaterial;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.material.FiltroMateriais;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

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

    @Override
    public Pagina<Material> pesquisar(FiltroMateriais filtro, PedidoDePagina pedido) {
        Specification<MaterialJpaEntity> criterios = (material, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filtro.tipo() != null) {
                condicoes.add(cb.equal(material.get("tipo"), filtro.tipo()));
            }
            if (filtro.busca() != null) {
                List<Predicate> alternativas = new ArrayList<>();
                alternativas.add(cb.like(cb.lower(material.get("descricao")),
                        PesquisaJpa.contem(filtro.busca()), PesquisaJpa.ESCAPE));
                if (filtro.digitosDoCodigo() != null) {
                    // só dígitos: não há curingas a escapar
                    alternativas.add(cb.like(material.get("codigo"), "%" + filtro.digitosDoCodigo() + "%"));
                }
                condicoes.add(cb.or(alternativas.toArray(new Predicate[0])));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
        Sort ordenacao = Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("codigo"));

        Page<MaterialJpaEntity> pagina = springDataRepository.findAll(criterios,
                PageRequest.of(pedido.pagina(), pedido.tamanho(), ordenacao));

        return new Pagina<>(pagina.getContent().stream().map(MaterialJpaEntity::paraDominio).toList(),
                pedido.pagina(), pedido.tamanho(), pagina.getTotalElements());
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
