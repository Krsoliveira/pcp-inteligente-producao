package com.krsoliveira.pcp.infrastructure.persistence;

import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import jakarta.persistence.criteria.Predicate;
import com.krsoliveira.pcp.domain.lote.FiltroLotes;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.LoteRepository;
import com.krsoliveira.pcp.domain.lote.StatusLote;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ADAPTADOR: implementa a porta {@link LoteRepository} usando JPA/PostgreSQL.
 */
@Repository
public class LoteRepositoryAdapter implements LoteRepository {

    /** Espaço de chaves do bloqueio consultivo da numeração de lotes de produção. */
    private static final long CHAVE_BLOQUEIO_LOTE_PRODUCAO = 7_400_000_000L;

    private final LoteSpringDataRepository springData;

    public LoteRepositoryAdapter(LoteSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public Lote salvar(Lote lote) {
        return springData.save(LoteJpaEntity.deDominio(lote)).paraDominio();
    }

    @Override
    public Optional<Lote> buscarPorId(UUID id) {
        return springData.findById(id).map(LoteJpaEntity::paraDominio);
    }

    @Override
    public Optional<Lote> buscarPorIdParaAtualizar(UUID id) {
        return springData.buscarParaAtualizar(id).map(LoteJpaEntity::paraDominio);
    }

    @Override
    public List<Lote> buscarPorIds(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return springData.findAllById(ids).stream().map(LoteJpaEntity::paraDominio).toList();
    }

    @Override
    public List<Lote> listarPorOrdens(Collection<UUID> ordemProducaoIds) {
        if (ordemProducaoIds.isEmpty()) return List.of();
        return springData.findByOrdemProducaoIdIn(ordemProducaoIds).stream()
                .map(LoteJpaEntity::paraDominio).toList();
    }

    @Override
    public List<Lote> listarDisponiveisPorMaterial(UUID materialId) {
        return springData.disponiveisPorMaterial(materialId).stream()
                .map(LoteJpaEntity::paraDominio).toList();
    }

    @Override
    public long contarPorStatus(StatusLote status) {
        return springData.countByStatus(status);
    }

    @Override
    public Pagina<Lote> pesquisar(FiltroLotes filtro, PedidoDePagina pedido) {
        Specification<LoteJpaEntity> criterios = (lote, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filtro.status() != null) {
                condicoes.add(cb.equal(lote.get("status"), filtro.status()));
            }
            if (filtro.materialId() != null) {
                condicoes.add(cb.equal(lote.get("materialId"), filtro.materialId()));
            }
            if (filtro.busca() != null) {
                condicoes.add(cb.like(cb.lower(lote.get("numeroLote")), PesquisaJpa.contem(filtro.busca()), PesquisaJpa.ESCAPE));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
        Sort ordenacao = Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("numeroLote"));
        Page<LoteJpaEntity> pagina = springData.findAll(criterios, PesquisaJpa.pagina(pedido.pagina(), pedido.tamanho(), ordenacao));
        return new Pagina<>(pagina.getContent().stream().map(LoteJpaEntity::paraDominio).toList(),
                pedido.pagina(), pedido.tamanho(), pagina.getTotalElements());
    }

    @Override
    public List<Lote> listarTodos() {
        return springData.findAll().stream()
                .map(LoteJpaEntity::paraDominio)
                .toList();
    }

    @Override
    public List<Lote> listarPorMaterial(UUID materialId) {
        return springData.findByMaterialId(materialId).stream().map(LoteJpaEntity::paraDominio).toList();
    }

    @Override
    public List<Lote> listarPorNotasFiscais(Collection<UUID> notaFiscalIds) {
        if (notaFiscalIds.isEmpty()) return List.of();
        return springData.findByNotaFiscalIdIn(notaFiscalIds).stream().map(LoteJpaEntity::paraDominio).toList();
    }

    @Override
    public boolean existeLote(UUID materialId, String numeroLote, String fornecedor) {
        return springData.existeLote(materialId, numeroLote, fornecedor == null ? "" : fornecedor);
    }

    @Override
    public List<Lote> listarPorOrdemProducao(UUID ordemProducaoId) {
        return springData.findByOrdemProducaoId(ordemProducaoId).stream()
                .map(LoteJpaEntity::paraDominio)
                .toList();
    }

    /**
     * Bloqueio consultivo por dia, válido até o fim da transação: conclusões simultâneas
     * no mesmo dia esperam uma pela outra e não colidem no número do lote.
     */
    @Override
    public int proximoSequencialProducao(LocalDate data) {
        springData.bloquear(CHAVE_BLOQUEIO_LOTE_PRODUCAO + data.toEpochDay());
        String prefixo = Lote.prefixoLoteProducao(data);
        return springData.ultimoLoteProducaoDoDia(prefixo)
                .filter(ultimo -> ultimo.length() == prefixo.length() + 4)
                .map(ultimo -> Integer.parseInt(ultimo.substring(prefixo.length())) + 1)
                .orElse(1);
    }
}
