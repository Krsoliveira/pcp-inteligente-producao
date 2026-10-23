package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.ordem.FiltroOrdens;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ADAPTADOR: implementa a porta {@link OrdemProducaoRepository} (declarada no
 * domínio) usando JPA/PostgreSQL. O domínio não sabe que esta classe existe.
 */
@Repository
public class OrdemProducaoRepositoryAdapter implements OrdemProducaoRepository {

    private final OrdemProducaoSpringDataRepository springData;

    public OrdemProducaoRepositoryAdapter(OrdemProducaoSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public OrdemProducao salvar(OrdemProducao ordem) {
        return springData.save(OrdemProducaoJpaEntity.deDominio(ordem)).paraDominio();
    }

    @Override
    public Optional<OrdemProducao> buscarPorId(UUID id) {
        return springData.findById(id).map(OrdemProducaoJpaEntity::paraDominio);
    }

    @Override
    public List<OrdemProducao> buscarPorIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return springData.findAllById(ids).stream().map(OrdemProducaoJpaEntity::paraDominio).toList();
    }

    @Override
    public List<OrdemProducao> listarTodas() {
        return springData.findAll().stream()
                .map(OrdemProducaoJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public Pagina<OrdemProducao> pesquisar(FiltroOrdens filtro, PedidoDePagina pedido) {
        Specification<OrdemProducaoJpaEntity> criterios = (ordem, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filtro.status() != null) {
                condicoes.add(cb.equal(ordem.get("status"), filtro.status()));
            }
            if (filtro.de() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(ordem.get("inicioPlanejado"), filtro.de()));
            }
            if (filtro.ate() != null) {
                condicoes.add(cb.lessThanOrEqualTo(ordem.get("inicioPlanejado"), filtro.ate()));
            }
            if (filtro.atrasadasEm() != null) {
                condicoes.add(ordem.get("status").in(statusAbertos()));
                condicoes.add(cb.lessThan(ordem.get("fimPlanejado"), filtro.atrasadasEm()));
            }
            if (filtro.busca() != null) {
                condicoes.add(cb.like(cb.lower(ordem.get("codigo")),
                        PesquisaJpa.contem(filtro.busca()), PesquisaJpa.ESCAPE));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
        Sort ordenacao = Sort.by(Sort.Order.desc("criadaEm"), Sort.Order.desc("codigo"));

        Page<OrdemProducaoJpaEntity> pagina = springData.findAll(criterios,
                PageRequest.of(pedido.pagina(), pedido.tamanho(), ordenacao));

        return new Pagina<>(pagina.getContent().stream().map(OrdemProducaoJpaEntity::paraDominio).toList(),
                pedido.pagina(), pedido.tamanho(), pagina.getTotalElements());
    }

    @Override
    public Map<StatusOrdemProducao, Long> contarPorStatus() {
        Map<StatusOrdemProducao, Long> contagem = new EnumMap<>(StatusOrdemProducao.class);
        for (Object[] linha : springData.contarAgrupadoPorStatus()) {
            contagem.put((StatusOrdemProducao) linha[0], (Long) linha[1]);
        }
        return contagem;
    }

    @Override
    public long contarAtrasadas(LocalDate hoje) {
        return springData.countByStatusInAndFimPlanejadoBefore(statusAbertos(), hoje);
    }

    private static List<StatusOrdemProducao> statusAbertos() {
        return Arrays.stream(StatusOrdemProducao.values())
                .filter(StatusOrdemProducao::estaAberta)
                .toList();
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return springData.existsByCodigo(codigo);
    }

    @Override
    public long contarTodas() {
        return springData.count();
    }
}
