-- V10: Genealogia de lotes (ADR-0011, entrega 3).
--
-- 1. Saldo do lote: quantidade inicial menos o que foi alocado a consumos.
-- 2. Alocação de lote: quanto de cada lote saiu em cada consumo de material — o elo
--    que liga o produto acabado aos lotes de matéria-prima e à nota fiscal.

-- ---------------------------------------------------------------------------
-- 1. Saldo
-- ---------------------------------------------------------------------------
ALTER TABLE lote ADD COLUMN saldo NUMERIC(12, 4);

-- Lotes existentes: sem alocações registradas, o saldo segue o status — consumido
-- vale zero; os demais, a quantidade inteira.
UPDATE lote SET saldo = CASE WHEN status = 'CONSUMIDO' THEN 0 ELSE quantidade END;

ALTER TABLE lote
    ALTER COLUMN saldo SET NOT NULL,
    ADD CONSTRAINT ck_lote_saldo CHECK (saldo >= 0 AND saldo <= quantidade);

COMMENT ON COLUMN lote.saldo IS 'Quantidade ainda disponível: quantidade menos as alocações a consumos.';

-- Busca de lotes disponíveis de um material por validade (FEFO).
CREATE INDEX idx_lote_material_disponivel ON lote (material_id, data_validade)
    WHERE status = 'DISPONIVEL' AND saldo > 0;

-- ---------------------------------------------------------------------------
-- 2. Alocação de lote
-- ---------------------------------------------------------------------------
CREATE TABLE alocacao_lote (
    id                  UUID           NOT NULL,
    consumo_material_id UUID           NOT NULL,
    lote_id             UUID           NOT NULL,
    quantidade          NUMERIC(12, 4) NOT NULL,
    criado_por          VARCHAR(150)   NOT NULL,
    criado_em           TIMESTAMPTZ    NOT NULL,

    CONSTRAINT pk_alocacao_lote PRIMARY KEY (id),
    CONSTRAINT fk_alocacao_consumo FOREIGN KEY (consumo_material_id) REFERENCES consumo_material (id),
    CONSTRAINT fk_alocacao_lote FOREIGN KEY (lote_id) REFERENCES lote (id),
    CONSTRAINT uk_alocacao_consumo_lote UNIQUE (consumo_material_id, lote_id),
    CONSTRAINT ck_alocacao_quantidade CHECK (quantidade > 0)
);

CREATE INDEX idx_alocacao_lote ON alocacao_lote (lote_id);
-- (consumo_material_id, lote_id) já é indexado pela restrição de unicidade.

COMMENT ON TABLE alocacao_lote IS
    'Genealogia: quanto de cada lote foi usado em cada consumo de material. Registros imutáveis.';
