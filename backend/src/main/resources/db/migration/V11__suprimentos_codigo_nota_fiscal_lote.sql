-- V11: Suprimentos (ADR-0012).
--
-- 1. Código do material: exatamente 9 dígitos, com a faixa definida pelo tipo:
--    103 = produto acabado, 105 = semiacabado, 110 = matéria-prima.
-- 2. Nota fiscal de entrada: cabeçalho próprio; os itens são os lotes de compra.
-- 3. Número do lote: até 20 caracteres (letras, dígitos e . / -). Compra usa o lote do
--    fornecedor; produção usa AAMMDD + sequência do dia.
--
-- Dados existentes são convertidos e cada troca de código ou de número fica registrada
-- na trilha de auditoria (de → para), como 'sistema:migracao-v11'.

-- ---------------------------------------------------------------------------
-- 1. Código do material
-- ---------------------------------------------------------------------------
CREATE TEMPORARY TABLE v11_codigo_material ON COMMIT DROP AS
WITH faixa AS (
    SELECT id, codigo, tipo, criado_em,
           CASE tipo WHEN 'PRODUTO_ACABADO' THEN '103'
                     WHEN 'SEMIACABADO'     THEN '105'
                     ELSE '110' END AS prefixo
      FROM material
), validos AS (
    -- Códigos que já seguem o padrão ficam como estão; os novos continuam depois deles.
    SELECT tipo, MAX(SUBSTRING(codigo, 4)::INT) AS ultimo
      FROM faixa
     WHERE codigo ~ ('^' || prefixo || '[0-9]{6}$')
     GROUP BY tipo
)
SELECT f.id,
       f.codigo AS anterior,
       f.prefixo || LPAD((COALESCE(v.ultimo, 0)
                          + ROW_NUMBER() OVER (PARTITION BY f.tipo ORDER BY f.criado_em, f.codigo))::TEXT,
                         6, '0') AS novo
  FROM faixa f
  LEFT JOIN validos v ON v.tipo = f.tipo
 WHERE f.codigo !~ ('^' || f.prefixo || '[0-9]{6}$');

INSERT INTO evento_auditoria (id, tipo_entidade, entidade_id, referencia, acao, usuario, ocorrido_em, detalhes)
SELECT gen_random_uuid(), 'MATERIAL', id, novo, 'ALTERADO', 'sistema:migracao-v11', NOW(),
       jsonb_build_object('codigo', jsonb_build_object('de', anterior, 'para', novo),
                          'motivo', 'Padronização do código em 9 dígitos por faixa (ADR-0012)')
  FROM v11_codigo_material;

UPDATE material m
   SET codigo = c.novo, atualizado_em = NOW(), atualizado_por = 'sistema:migracao-v11'
  FROM v11_codigo_material c
 WHERE m.id = c.id;

ALTER TABLE material ALTER COLUMN codigo TYPE VARCHAR(9);
ALTER TABLE material
    ADD CONSTRAINT ck_material_codigo CHECK (
        (tipo = 'PRODUTO_ACABADO' AND codigo ~ '^103[0-9]{6}$')
        OR (tipo = 'SEMIACABADO'  AND codigo ~ '^105[0-9]{6}$')
        OR (tipo = 'MATERIA_PRIMA' AND codigo ~ '^110[0-9]{6}$')
    );

COMMENT ON COLUMN material.codigo IS
    '9 dígitos; faixa por tipo: 103 produto acabado, 105 semiacabado, 110 matéria-prima. Exibido como 103.000.001.';

-- ---------------------------------------------------------------------------
-- 2. Nota fiscal de entrada
-- ---------------------------------------------------------------------------
CREATE TABLE nota_fiscal_entrada (
    id               UUID         NOT NULL,
    fornecedor       VARCHAR(150) NOT NULL,
    numero           VARCHAR(44)  NOT NULL,
    data_emissao     DATE         NOT NULL,
    data_recebimento DATE         NOT NULL,
    criado_em        TIMESTAMPTZ  NOT NULL,
    atualizado_em    TIMESTAMPTZ  NOT NULL,
    criado_por       VARCHAR(150) NOT NULL,
    atualizado_por   VARCHAR(150) NOT NULL,

    CONSTRAINT pk_nota_fiscal_entrada PRIMARY KEY (id),
    CONSTRAINT ck_nota_fiscal_datas CHECK (data_recebimento >= data_emissao)
);

-- A mesma nota do mesmo fornecedor não entra duas vezes.
CREATE UNIQUE INDEX uk_nota_fiscal_fornecedor_numero ON nota_fiscal_entrada (LOWER(fornecedor), numero);
CREATE INDEX idx_nota_fiscal_recebimento ON nota_fiscal_entrada (data_recebimento DESC);

COMMENT ON TABLE nota_fiscal_entrada IS
    'Notas fiscais de compra. Os itens são os lotes de compra (lote.nota_fiscal_entrada_id).';

-- Lotes de compra existentes: uma nota por fornecedor + número.
INSERT INTO nota_fiscal_entrada (id, fornecedor, numero, data_emissao, data_recebimento,
                                 criado_em, atualizado_em, criado_por, atualizado_por)
SELECT gen_random_uuid(), MIN(fornecedor), nota_fiscal, MIN(data_emissao_nf), MIN(data_recebimento),
       MIN(criado_em), MIN(criado_em),
       (ARRAY_AGG(criado_por ORDER BY criado_em))[1], (ARRAY_AGG(criado_por ORDER BY criado_em))[1]
  FROM lote
 WHERE nota_fiscal IS NOT NULL
 GROUP BY LOWER(fornecedor), nota_fiscal;

ALTER TABLE lote
    ADD COLUMN nota_fiscal_entrada_id UUID,
    ADD CONSTRAINT fk_lote_nota_fiscal FOREIGN KEY (nota_fiscal_entrada_id) REFERENCES nota_fiscal_entrada (id);

UPDATE lote l
   SET nota_fiscal_entrada_id = n.id
  FROM nota_fiscal_entrada n
 WHERE l.nota_fiscal IS NOT NULL
   AND LOWER(l.fornecedor) = LOWER(n.fornecedor)
   AND l.nota_fiscal = n.numero;

CREATE INDEX idx_lote_nota_fiscal ON lote (nota_fiscal_entrada_id) WHERE nota_fiscal_entrada_id IS NOT NULL;

-- Todo lote de compra pertence a uma nota; lote de produção, a nenhuma.
ALTER TABLE lote DROP CONSTRAINT ck_lote_origem;
ALTER TABLE lote
    ADD CONSTRAINT ck_lote_origem CHECK (
        (ordem_producao_id IS NOT NULL
            AND nota_fiscal_entrada_id IS NULL
            AND fornecedor IS NULL AND nota_fiscal IS NULL
            AND data_emissao_nf IS NULL AND data_recebimento IS NULL)
        OR (ordem_producao_id IS NULL
            AND nota_fiscal_entrada_id IS NOT NULL
            AND fornecedor IS NOT NULL AND nota_fiscal IS NOT NULL
            AND data_emissao_nf IS NOT NULL AND data_recebimento IS NOT NULL
            AND data_recebimento >= data_emissao_nf)
    );

-- ---------------------------------------------------------------------------
-- 3. Número do lote
-- ---------------------------------------------------------------------------
-- Números fora do novo padrão (ex.: MAT-PA-MOTOR-A200-202503-001, com 28 caracteres):
--   produção → AAMMDD da fabricação + sequência do dia (continua após os já válidos);
--   compra   → MIG-AAMMDD-NNN (o lote do fornecedor não era registrado antes).
CREATE TEMPORARY TABLE v11_numero_lote ON COMMIT DROP AS
WITH producao_valida AS (
    SELECT data_fabricacao, MAX(SUBSTRING(numero_lote, 7)::INT) AS ultimo
      FROM lote
     WHERE ordem_producao_id IS NOT NULL AND numero_lote ~ '^[0-9]{10}$'
     GROUP BY data_fabricacao
), invalidos AS (
    SELECT * FROM lote WHERE numero_lote !~ '^[A-Z0-9./-]{1,20}$'
)
SELECT i.id, i.numero_lote AS anterior,
       CASE WHEN i.ordem_producao_id IS NOT NULL THEN
                TO_CHAR(i.data_fabricacao, 'YYMMDD')
                || LPAD((COALESCE(p.ultimo, 0)
                         + ROW_NUMBER() OVER (PARTITION BY i.ordem_producao_id IS NOT NULL, i.data_fabricacao
                                              ORDER BY i.criado_em, i.numero_lote))::TEXT, 4, '0')
            ELSE
                'MIG-' || TO_CHAR(i.data_recebimento, 'YYMMDD') || '-'
                || LPAD(ROW_NUMBER() OVER (PARTITION BY i.ordem_producao_id IS NOT NULL, i.material_id, i.fornecedor,
                                                        i.data_recebimento
                                           ORDER BY i.criado_em, i.numero_lote)::TEXT, 3, '0')
       END AS novo
  FROM invalidos i
  LEFT JOIN producao_valida p ON p.data_fabricacao = i.data_fabricacao AND i.ordem_producao_id IS NOT NULL;

INSERT INTO evento_auditoria (id, tipo_entidade, entidade_id, referencia, acao, usuario, ocorrido_em, detalhes)
SELECT gen_random_uuid(), 'LOTE', id, novo, 'ALTERADO', 'sistema:migracao-v11', NOW(),
       jsonb_build_object('numeroLote', jsonb_build_object('de', anterior, 'para', novo),
                          'motivo', 'Número de lote com até 20 caracteres (ADR-0012)')
  FROM v11_numero_lote;

ALTER TABLE lote DROP CONSTRAINT uq_lote_numero_lote;

UPDATE lote l
   SET numero_lote = n.novo, atualizado_em = NOW(), atualizado_por = 'sistema:migracao-v11'
  FROM v11_numero_lote n
 WHERE l.id = n.id;

ALTER TABLE lote ALTER COLUMN numero_lote TYPE VARCHAR(20);
ALTER TABLE lote ADD CONSTRAINT ck_lote_numero CHECK (numero_lote ~ '^[A-Z0-9./-]{1,20}$');

-- A nota agora tem itens: o mesmo material pode vir em mais de um lote na mesma nota.
-- A unicidade passa a ser da nota (uk_nota_fiscal_fornecedor_numero) e do lote (abaixo).
DROP INDEX uq_lote_entrada_compra;

-- Fornecedores diferentes podem usar o mesmo número de lote; o mesmo fornecedor, não.
CREATE UNIQUE INDEX uk_lote_material_numero_fornecedor
    ON lote (material_id, numero_lote, COALESCE(fornecedor, ''));
-- Lote de produção (AAMMDD + sequência) é único no sistema todo.
CREATE UNIQUE INDEX uk_lote_producao_numero ON lote (numero_lote) WHERE ordem_producao_id IS NOT NULL;

COMMENT ON COLUMN lote.numero_lote IS
    'Até 20 caracteres. Compra: lote do fornecedor. Produção: AAMMDD + sequência do dia (ex.: 2610030001).';
