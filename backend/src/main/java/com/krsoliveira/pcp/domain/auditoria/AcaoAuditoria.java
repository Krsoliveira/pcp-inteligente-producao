package com.krsoliveira.pcp.domain.auditoria;

/**
 * Ações de negócio registradas na trilha de auditoria.
 * São fatos que um auditor lê ("ordem liberada"), não operações técnicas de banco.
 */
public enum AcaoAuditoria {
    CRIADO,
    ALTERADO,
    STATUS_ALTERADO,
    ATIVADA,
    OBSOLETADA,
    CONSUMO_REGISTRADO,
    ORDEM_CONCLUIDA,
    LOTE_GERADO,
    ENTRADA_REGISTRADA,
    USUARIO_REGISTRADO
}
