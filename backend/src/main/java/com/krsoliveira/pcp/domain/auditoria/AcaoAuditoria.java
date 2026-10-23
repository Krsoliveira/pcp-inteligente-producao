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
    /** Parte do saldo do lote foi usada num consumo (genealogia). */
    LOTE_ALOCADO,
    ENTRADA_REGISTRADA,
    USUARIO_REGISTRADO,
    /** Uma entidade foi excluída (ex.: perfil de acesso sem usuários). */
    EXCLUIDO,
    /** Um usuário teve o perfil de acesso trocado (ADR-0020). */
    PERFIL_ATRIBUIDO
}
