-- V9: Rastreabilidade e auditoria (ADR-0011).
--
-- 1. Assinatura em todo registro: quem criou e quem alterou por último.
-- 2. Datas da nota fiscal na entrada de material (emissão e recebimento).
-- 3. Trilha de auditoria imutável (append-only).
--
-- Registros já existentes recebem o responsável 'sistema:migracao-v9', deixando explícito
-- que foram criados antes de o sistema registrar autoria.

-- ---------------------------------------------------------------------------
-- 1. Assinatura
-- ---------------------------------------------------------------------------
ALTER TABLE material
    ADD COLUMN criado_por     VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9',
    ADD COLUMN atualizado_por VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9';

ALTER TABLE lista_tecnica
    ADD COLUMN criada_por     VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9',
    ADD COLUMN atualizada_por VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9';

ALTER TABLE ordem_producao
    ADD COLUMN criada_por     VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9',
    ADD COLUMN atualizada_por VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9';

ALTER TABLE tipo_ordem
    ADD COLUMN criado_por     VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9',
    ADD COLUMN atualizado_por VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9';

ALTER TABLE consumo_material
    ADD COLUMN criado_por     VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9',
    ADD COLUMN atualizado_por VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9',
    ADD COLUMN atualizado_em  TIMESTAMPTZ;
UPDATE consumo_material SET atualizado_em = COALESCE(justificado_em, criado_em);
ALTER TABLE consumo_material ALTER COLUMN atualizado_em SET NOT NULL;

ALTER TABLE lote
    ADD COLUMN criado_por     VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9',
    ADD COLUMN atualizado_por VARCHAR(150) NOT NULL DEFAULT 'sistema:migracao-v9',
    ADD COLUMN atualizado_em  TIMESTAMPTZ;
UPDATE lote SET atualizado_em = criado_em;
ALTER TABLE lote ALTER COLUMN atualizado_em SET NOT NULL;

-- O DEFAULT só serve para preencher os registros existentes: daqui em diante a
-- aplicação sempre informa o responsável.
ALTER TABLE material         ALTER COLUMN criado_por DROP DEFAULT, ALTER COLUMN atualizado_por DROP DEFAULT;
ALTER TABLE lista_tecnica    ALTER COLUMN criada_por DROP DEFAULT, ALTER COLUMN atualizada_por DROP DEFAULT;
ALTER TABLE ordem_producao   ALTER COLUMN criada_por DROP DEFAULT, ALTER COLUMN atualizada_por DROP DEFAULT;
ALTER TABLE tipo_ordem       ALTER COLUMN criado_por DROP DEFAULT, ALTER COLUMN atualizado_por DROP DEFAULT;
ALTER TABLE consumo_material ALTER COLUMN criado_por DROP DEFAULT, ALTER COLUMN atualizado_por DROP DEFAULT;
ALTER TABLE lote             ALTER COLUMN criado_por DROP DEFAULT, ALTER COLUMN atualizado_por DROP DEFAULT;

CREATE INDEX idx_ordem_producao_criada_em ON ordem_producao (criada_em);
CREATE INDEX idx_lote_criado_em           ON lote (criado_em);

-- ---------------------------------------------------------------------------
-- 2. Datas da nota fiscal (lotes de compra)
-- ---------------------------------------------------------------------------
ALTER TABLE lote
    ADD COLUMN data_emissao_nf  DATE,
    ADD COLUMN data_recebimento DATE;

-- Entradas registradas antes desta migração: a melhor informação disponível é a data
-- em que foram lançadas no sistema.
UPDATE lote
   SET data_emissao_nf  = criado_em::date,
       data_recebimento = criado_em::date
 WHERE nota_fiscal IS NOT NULL;

ALTER TABLE lote DROP CONSTRAINT ck_lote_origem;
ALTER TABLE lote
    ADD CONSTRAINT ck_lote_origem CHECK (
        (ordem_producao_id IS NOT NULL
            AND fornecedor IS NULL AND nota_fiscal IS NULL
            AND data_emissao_nf IS NULL AND data_recebimento IS NULL)
        OR (ordem_producao_id IS NULL
            AND fornecedor IS NOT NULL AND nota_fiscal IS NOT NULL
            AND data_emissao_nf IS NOT NULL AND data_recebimento IS NOT NULL
            AND data_recebimento >= data_emissao_nf)
    );

CREATE INDEX idx_lote_data_recebimento ON lote (data_recebimento) WHERE nota_fiscal IS NOT NULL;

COMMENT ON COLUMN lote.data_emissao_nf  IS 'Data de emissão da nota fiscal (apenas lotes de compra).';
COMMENT ON COLUMN lote.data_recebimento IS 'Data em que o material chegou fisicamente (apenas lotes de compra).';

-- ---------------------------------------------------------------------------
-- 3. Trilha de auditoria imutável
-- ---------------------------------------------------------------------------
CREATE TABLE evento_auditoria (
    id            UUID         NOT NULL,
    tipo_entidade VARCHAR(30)  NOT NULL,
    entidade_id   UUID         NOT NULL,
    referencia    VARCHAR(80)  NOT NULL,
    acao          VARCHAR(40)  NOT NULL,
    usuario       VARCHAR(150) NOT NULL,
    ocorrido_em   TIMESTAMPTZ  NOT NULL,
    detalhes      JSONB        NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT pk_evento_auditoria PRIMARY KEY (id)
);

CREATE INDEX idx_evento_entidade ON evento_auditoria (tipo_entidade, entidade_id, ocorrido_em DESC);
CREATE INDEX idx_evento_ocorrido ON evento_auditoria (ocorrido_em DESC);
CREATE INDEX idx_evento_usuario  ON evento_auditoria (usuario, ocorrido_em DESC);

COMMENT ON TABLE evento_auditoria IS
    'Trilha de auditoria append-only: quem fez o quê, em qual registro e quando. UPDATE e DELETE são bloqueados.';

-- Imutabilidade garantida no banco, não só na aplicação.
CREATE FUNCTION fn_evento_auditoria_imutavel() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'A trilha de auditoria é imutável: % não é permitido.', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_evento_auditoria_imutavel
    BEFORE UPDATE OR DELETE ON evento_auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_evento_auditoria_imutavel();

-- TRUNCATE não dispara gatilhos por linha; bloqueado separadamente.
CREATE TRIGGER trg_evento_auditoria_sem_truncate
    BEFORE TRUNCATE ON evento_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION fn_evento_auditoria_imutavel();
