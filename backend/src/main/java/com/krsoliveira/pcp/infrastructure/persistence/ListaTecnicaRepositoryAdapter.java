package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.lista.FiltroListasTecnicas;
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
 * Adaptador: implementa a porta {@link ListaTecnicaRepository} usando Spring Data JPA.
 */
@Repository
public class ListaTecnicaRepositoryAdapter implements ListaTecnicaRepository {

    private final ListaTecnicaSpringDataRepository springDataRepository;

    public ListaTecnicaRepositoryAdapter(ListaTecnicaSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public void salvar(ListaTecnica lista) {
        springDataRepository.save(ListaTecnicaJpaEntity.deDominio(lista));
    }

    @Override
    public Optional<ListaTecnica> buscarPorId(UUID id) {
        return springDataRepository.findById(id).map(ListaTecnicaJpaEntity::paraDominio);
    }

    @Override
    public List<ListaTecnica> listarTodas() {
        return springDataRepository.findAll().stream()
                .map(ListaTecnicaJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public Pagina<ListaTecnica> pesquisar(FiltroListasTecnicas filtro, PedidoDePagina pedido) {
        Specification<ListaTecnicaJpaEntity> criterios = (lista, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filtro.materialId() != null) {
                condicoes.add(cb.equal(lista.get("materialId"), filtro.materialId()));
            }
            if (filtro.status() != null) {
                condicoes.add(cb.equal(lista.get("status"), filtro.status()));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
        Sort ordenacao = Sort.by(Sort.Order.desc("criadaEm"), Sort.Order.desc("versao"));

        Page<ListaTecnicaJpaEntity> pagina = springDataRepository.findAll(criterios,
                PageRequest.of(pedido.pagina(), pedido.tamanho(), ordenacao));

        return new Pagina<>(pagina.getContent().stream().map(ListaTecnicaJpaEntity::paraDominio).toList(),
                pedido.pagina(), pedido.tamanho(), pagina.getTotalElements());
    }

    @Override
    public List<ListaTecnica> listarPorMaterial(UUID materialId) {
        return springDataRepository.findByMaterialId(materialId).stream()
                .map(ListaTecnicaJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public Optional<ListaTecnica> buscarAtivaParaMaterial(UUID materialId) {
        return springDataRepository
                .findByMaterialIdAndStatus(materialId, StatusListaTecnica.ATIVA)
                .map(ListaTecnicaJpaEntity::paraDominio);
    }

    @Override
    public boolean existeVersaoParaMaterial(UUID materialId, String versao) {
        return springDataRepository.existsByMaterialIdAndVersao(materialId, versao);
    }
}
