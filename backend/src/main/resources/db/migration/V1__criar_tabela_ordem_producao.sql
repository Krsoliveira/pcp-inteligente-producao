-- V1: tabela de ordens de produção.
-- O banco também valida as invariantes críticas (CHECKs e UNIQUE):
-- defesa em profundidade — mesmo que um bug contorne o domínio, o dado
-- inconsistente não entra.

CREATE TABLE ordem_producao (
    id                UUID PRIMARY KEY,
    codigo            VARCHAR(30)  NOT NULL,
    produto           VARCHAR(120) NOT NULL,
    quantidade        INTEGER      NOT NULL,
    inicio_planejado  DATE         NOT NULL,
    fim_planejado     DATE         NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    criada_em         TIMESTAMPTZ  NOT NULL,
    atualizada_em     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_ordem_producao_codigo UNIQUE (codigo),
    CONSTRAINT chk_ordem_producao_quantidade CHECK (quantidade > 0),
    CONSTRAINT chk_ordem_producao_periodo CHECK (fim_planejado >= inicio_planejado),
    CONSTRAINT chk_ordem_producao_status CHECK (
        status IN ('PLANEJADA', 'LIBERADA', 'EM_PRODUCAO', 'CONCLUIDA', 'CANCELADA')
    )
);

-- Consultas do dashboard filtrarão por status com frequência.
CREATE INDEX idx_ordem_producao_status ON ordem_producao (status);
