package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.FiltroEventos;
import com.krsoliveira.pcp.domain.auditoria.OrdemDosEventos;
import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.auditoria.TrilhaDeAuditoria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Adaptador JPA da trilha de auditoria: só inserção e consulta. */
@Component
public class TrilhaDeAuditoriaAdapter implements TrilhaDeAuditoria {

    private final EventoAuditoriaSpringDataRepository springData;

    public TrilhaDeAuditoriaAdapter(EventoAuditoriaSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public void registrar(List<EventoAuditoria> eventos) {
        springData.saveAll(eventos.stream().map(EventoAuditoriaJpaEntity::deDominio).toList());
    }

    @Override
    public Pagina<EventoAuditoria> consultar(FiltroEventos filtro, OrdemDosEventos ordem, int pagina, int tamanho) {
        // O id desempata eventos do mesmo instante, para a ordem (e a paginação sobre ela) ser sempre a mesma.
        Sort ordenacao = ordem == OrdemDosEventos.MAIS_ANTIGOS_PRIMEIRO
                ? Sort.by(Sort.Order.asc("ocorridoEm"), Sort.Order.asc("id"))
                : Sort.by(Sort.Order.desc("ocorridoEm"), Sort.Order.desc("id"));
        PageRequest paginacao = PesquisaJpa.pagina(pagina, tamanho, ordenacao);
        Page<EventoAuditoriaJpaEntity> resultado = springData.findAll(especificacao(filtro), paginacao);
        return new Pagina<>(resultado.map(EventoAuditoriaJpaEntity::paraDominio).getContent(),
                pagina, tamanho, resultado.getTotalElements());
    }

    /** Monta apenas as condições informadas — filtros nulos são ignorados. */
    private static Specification<EventoAuditoriaJpaEntity> especificacao(FiltroEventos f) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (f.tipoEntidade() != null) condicoes.add(cb.equal(raiz.get("tipoEntidade"), f.tipoEntidade()));
            if (f.entidadeId() != null) condicoes.add(cb.equal(raiz.get("entidadeId"), f.entidadeId()));
            if (f.usuario() != null && !f.usuario().isBlank()) {
                condicoes.add(cb.equal(cb.lower(raiz.get("usuario")), f.usuario().trim().toLowerCase()));
            }
            if (f.acao() != null) condicoes.add(cb.equal(raiz.get("acao"), f.acao()));
            if (f.de() != null) condicoes.add(cb.greaterThanOrEqualTo(raiz.get("ocorridoEm"), f.de()));
            if (f.ate() != null) condicoes.add(cb.lessThan(raiz.get("ocorridoEm"), f.ate()));
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
    }
}
