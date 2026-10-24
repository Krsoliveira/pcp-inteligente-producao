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
    /** Um item fora da lista técnica foi adicionado ao consumo da ordem e já registrado (ADR-0025). */
    CONSUMO_ADICIONADO,
    ORDEM_CONCLUIDA,
    LOTE_GERADO,
    /** Parte do saldo do lote foi usada num consumo (genealogia). */
    LOTE_ALOCADO,
    ENTRADA_REGISTRADA,
    USUARIO_REGISTRADO,
    /** Uma entidade foi excluída (ex.: perfil de acesso sem usuários). */
    EXCLUIDO,
    /** Um usuário teve o perfil de acesso trocado (ADR-0020). */
    PERFIL_ATRIBUIDO,
    /** O próprio usuário trocou a senha (a senha nunca vai para a trilha). */
    SENHA_ALTERADA,
    /** Um gestor redefiniu a senha de um usuário. */
    SENHA_REDEFINIDA
}
