package com.krsoliveira.pcp.domain.auditoria;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fato registrado na trilha de auditoria: quem fez o quê, em qual registro e quando.
 *
 * Imutável por definição — uma vez registrado, nunca é alterado nem apagado (o banco
 * também bloqueia UPDATE/DELETE). Os {@code detalhes} guardam os dados relevantes da
 * ação, inclusive valores "de → para" quando algo muda.
 */
public final class EventoAuditoria {

    private final UUID id;
    private final TipoEntidade tipoEntidade;
    private final UUID entidadeId;
    private final String referencia;
    private final AcaoAuditoria acao;
    private final String usuario;
    private final Instant ocorridoEm;
    private final Map<String, Object> detalhes;

    private EventoAuditoria(UUID id, TipoEntidade tipoEntidade, UUID entidadeId, String referencia,
                            AcaoAuditoria acao, String usuario, Instant ocorridoEm,
                            Map<String, Object> detalhes) {
        this.id = id;
        this.tipoEntidade = tipoEntidade;
        this.entidadeId = entidadeId;
        this.referencia = referencia;
        this.acao = acao;
        this.usuario = usuario;
        this.ocorridoEm = ocorridoEm;
        this.detalhes = Collections.unmodifiableMap(new LinkedHashMap<>(detalhes));
    }

    /**
     * Novo evento. {@code referencia} é o identificador legível do registro (código da
     * ordem, número do lote...), para a trilha ser compreensível sem consultar o banco.
     */
    public static EventoAuditoria registrar(TipoEntidade tipoEntidade, UUID entidadeId, String referencia,
                                            AcaoAuditoria acao, String usuario, Instant ocorridoEm,
                                            Map<String, Object> detalhes) {
        if (tipoEntidade == null || entidadeId == null || acao == null || ocorridoEm == null) {
            throw new RegraDeNegocioException("Evento de auditoria incompleto.");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new RegraDeNegocioException("Evento de auditoria sem usuário responsável.");
        }
        return new EventoAuditoria(UUID.randomUUID(), tipoEntidade, entidadeId,
                referencia == null ? "" : referencia, acao, usuario, ocorridoEm,
                detalhes == null ? Map.of() : detalhes);
    }

    public static EventoAuditoria reconstituir(UUID id, TipoEntidade tipoEntidade, UUID entidadeId,
                                               String referencia, AcaoAuditoria acao, String usuario,
                                               Instant ocorridoEm, Map<String, Object> detalhes) {
        return new EventoAuditoria(id, tipoEntidade, entidadeId, referencia, acao, usuario, ocorridoEm,
                detalhes == null ? Map.of() : detalhes);
    }

    public UUID getId() { return id; }
    public TipoEntidade getTipoEntidade() { return tipoEntidade; }
    public UUID getEntidadeId() { return entidadeId; }
    public String getReferencia() { return referencia; }
    public AcaoAuditoria getAcao() { return acao; }
    public String getUsuario() { return usuario; }
    public Instant getOcorridoEm() { return ocorridoEm; }
    public Map<String, Object> getDetalhes() { return detalhes; }
}
