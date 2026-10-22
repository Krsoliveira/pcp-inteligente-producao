package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntrada;
import com.krsoliveira.pcp.domain.notafiscal.NotaFiscalEntradaRepository;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.notafiscal.FiltroNotasFiscais;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** ADAPTADOR: implementa a porta {@link NotaFiscalEntradaRepository} com JPA/PostgreSQL. */
@Repository
public class NotaFiscalEntradaRepositoryAdapter implements NotaFiscalEntradaRepository {

    private final NotaFiscalEntradaSpringDataRepository springData;

    public NotaFiscalEntradaRepositoryAdapter(NotaFiscalEntradaSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public void salvar(NotaFiscalEntrada nota) {
        springData.save(NotaFiscalEntradaJpaEntity.deDominio(nota));
    }

    @Override
    public Optional<NotaFiscalEntrada> buscarPorId(UUID id) {
        return springData.findById(id).map(NotaFiscalEntradaJpaEntity::paraDominio);
    }

    @Override
    public boolean existe(String fornecedor, String numero) {
        return springData.existe(fornecedor, numero);
    }

    @Override
    public Pagina<NotaFiscalEntrada> pesquisar(FiltroNotasFiscais filtro, PedidoDePagina pedido) {
        Specification<NotaFiscalEntradaJpaEntity> criterios = (nota, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filtro.de() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(nota.get("dataRecebimento"), filtro.de()));
            }
            if (filtro.ate() != null) {
                condicoes.add(cb.lessThanOrEqualTo(nota.get("dataRecebimento"), filtro.ate()));
            }
            if (filtro.busca() != null) {
                String padrao = PesquisaJpa.contem(filtro.busca());
                // a nota também é encontrada pelo número de um dos lotes que ela trouxe
                Subquery<UUID> lotesDaNota = consulta.subquery(UUID.class);
                Root<LoteJpaEntity> lote = lotesDaNota.from(LoteJpaEntity.class);
                lotesDaNota.select(lote.<UUID>get("id")).where(
                        cb.equal(lote.get("notaFiscalId"), nota.get("id")),
                        cb.like(cb.lower(lote.get("numeroLote")), padrao, PesquisaJpa.ESCAPE));
                condicoes.add(cb.or(
                        cb.like(cb.lower(nota.get("numero")), padrao, PesquisaJpa.ESCAPE),
                        cb.like(cb.lower(nota.get("fornecedor")), padrao, PesquisaJpa.ESCAPE),
                        cb.exists(lotesDaNota)));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
        Sort ordenacao = Sort.by(Sort.Order.desc("dataRecebimento"), Sort.Order.desc("criadoEm"));

        Page<NotaFiscalEntradaJpaEntity> pagina = springData.findAll(criterios,
                PageRequest.of(pedido.pagina(), pedido.tamanho(), ordenacao));

        return new Pagina<>(pagina.getContent().stream().map(NotaFiscalEntradaJpaEntity::paraDominio).toList(),
                pedido.pagina(), pedido.tamanho(), pagina.getTotalElements());
    }

    @Override
    public List<NotaFiscalEntrada> listarPorRecebimento(LocalDate de, LocalDate ate) {
        return springData.porRecebimento(de, ate).stream().map(NotaFiscalEntradaJpaEntity::paraDominio).toList();
    }
}
