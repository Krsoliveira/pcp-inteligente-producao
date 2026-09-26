-- =============================================================
-- V6 — Fase 5b: TipoOrdem, Lote e ConsumoMaterial
--
-- ADR-0007: expande o domínio para suportar rastreabilidade
-- industrial completa — categorização de ordens, geração de
-- lote ao concluir e registro de consumo real de materiais.
-- =============================================================

-- ---------------------------------------------------------------
-- tipo_ordem — categorias de ordem definidas pelo usuário
-- Exemplos: Produção Normal, Manutenção, Revenda, Retrabalho
-- ---------------------------------------------------------------
CREATE TABLE tipo_ordem (
    id            UUID         NOT NULL,
    nome          VARCHAR(60)  NOT NULL,
    descricao     VARCHAR(200),
    cor           VARCHAR(7)   NOT NULL DEFAULT '#1565c0',
    criado_em     TIMESTAMPTZ  NOT NULL,
    atualizado_em TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_tipo_ordem PRIMARY KEY (id),
    CONSTRAINT uq_tipo_ordem_nome UNIQUE (nome)
);

-- ---------------------------------------------------------------
-- lote — gerado automaticamente ao concluir uma ordem
-- Formato do número: MAT-{codigo}-{yyyyMM}-{seq:03d}
-- ---------------------------------------------------------------
CREATE TABLE lote (
    id                UUID           NOT NULL,
    numero_lote       VARCHAR(40)    NOT NULL,
    material_id       UUID           NOT NULL,
    ordem_producao_id UUID,
    quantidade        NUMERIC(12, 4) NOT NULL,
    unidade_de_medida VARCHAR(10)    NOT NULL,
    data_fabricacao   DATE           NOT NULL,
    data_validade     DATE           NOT NULL,
    status            VARCHAR(15)    NOT NULL,
    criado_em         TIMESTAMPTZ    NOT NULL,

    CONSTRAINT pk_lote PRIMARY KEY (id),
    CONSTRAINT uq_lote_numero_lote UNIQUE (numero_lote),
    CONSTRAINT fk_lote_material FOREIGN KEY (material_id) REFERENCES material (id),
    CONSTRAINT fk_lote_ordem FOREIGN KEY (ordem_producao_id) REFERENCES ordem_producao (id),
    CONSTRAINT ck_lote_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_lote_validade CHECK (data_validade >= data_fabricacao),
    CONSTRAINT ck_lote_status CHECK (status IN ('DISPONIVEL', 'BLOQUEADO', 'CONSUMIDO', 'VENCIDO'))
);

CREATE INDEX idx_lote_material    ON lote (material_id);
CREATE INDEX idx_lote_ordem       ON lote (ordem_producao_id);
CREATE INDEX idx_lote_status      ON lote (status);

-- ---------------------------------------------------------------
-- consumo_material — consumo real por componente por ordem
-- Projetado automaticamente na criação da ordem a partir da BOM.
-- O operador preenche quantidade_consumida; desvio exige justificativa.
-- ---------------------------------------------------------------
CREATE TABLE consumo_material (
    id                   UUID           NOT NULL,
    ordem_producao_id    UUID           NOT NULL,
    material_id          UUID           NOT NULL,
    quantidade_planejada NUMERIC(12, 4) NOT NULL,
    quantidade_consumida NUMERIC(12, 4),
    unidade_de_medida    VARCHAR(10)    NOT NULL,
    justificativa        TEXT,
    justificado_por      VARCHAR(150),
    justificado_em       TIMESTAMPTZ,
    criado_em            TIMESTAMPTZ    NOT NULL,

    CONSTRAINT pk_consumo_material PRIMARY KEY (id),
    CONSTRAINT fk_consumo_ordem    FOREIGN KEY (ordem_producao_id) REFERENCES ordem_producao (id),
    CONSTRAINT fk_consumo_material FOREIGN KEY (material_id) REFERENCES material (id),
    CONSTRAINT ck_consumo_planejada CHECK (quantidade_planejada > 0)
);

CREATE INDEX idx_consumo_ordem ON consumo_material (ordem_producao_id);

-- ---------------------------------------------------------------
-- ordem_producao — colunas adicionadas na Fase 5b
-- ---------------------------------------------------------------
ALTER TABLE ordem_producao
    ADD COLUMN quantidade_produzida NUMERIC(12, 4),
    ADD COLUMN tipo_ordem_id UUID REFERENCES tipo_ordem (id);
