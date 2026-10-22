package com.krsoliveira.pcp.infrastructure.persistence;

import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Mapeamento JPA da trilha de auditoria. {@code @Immutable}: o Hibernate nunca gera
 * UPDATE para esta entidade (o banco também bloqueia por gatilho).
 */
@Entity
@Immutable
@Table(name = "evento_auditoria")
public class EventoAuditoriaJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entidade", nullable = false, length = 30)
    private TipoEntidade tipoEntidade;

    @Column(name = "entidade_id", nullable = false)
    private UUID entidadeId;

    @Column(nullable = false, length = 80)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcaoAuditoria acao;

    @Column(nullable = false, length = 150)
    private String usuario;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> detalhes;

    protected EventoAuditoriaJpaEntity() {}

    public static EventoAuditoriaJpaEntity deDominio(EventoAuditoria evento) {
        EventoAuditoriaJpaEntity entity = new EventoAuditoriaJpaEntity();
        entity.id = evento.getId();
        entity.tipoEntidade = evento.getTipoEntidade();
        entity.entidadeId = evento.getEntidadeId();
        entity.referencia = truncar(evento.getReferencia(), 80);
        entity.acao = evento.getAcao();
        entity.usuario = evento.getUsuario();
        entity.ocorridoEm = evento.getOcorridoEm();
        entity.detalhes = evento.getDetalhes();
        return entity;
    }

    public EventoAuditoria paraDominio() {
        return EventoAuditoria.reconstituir(id, tipoEntidade, entidadeId, referencia, acao, usuario,
                ocorridoEm, detalhes);
    }

    private static String truncar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
