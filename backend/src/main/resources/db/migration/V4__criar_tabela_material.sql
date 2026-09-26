-- V4: Tabela de materiais (Fase 5a)
--
-- Introduz o conceito de Material, substituindo o campo produto VARCHAR de
-- ordem_producao (que será removido na migração V7).
-- Um material pode ser PRODUTO_ACABADO, SEMIACABADO ou MATERIA_PRIMA.
-- Apenas os dois primeiros podem ter uma Lista Técnica associada.

CREATE TABLE material (
    id               UUID         NOT NULL,
    codigo           VARCHAR(30)  NOT NULL,
    descricao        VARCHAR(200) NOT NULL,
    tipo             VARCHAR(20)  NOT NULL,
    unidade_de_medida VARCHAR(10) NOT NULL,
    criado_em        TIMESTAMPTZ  NOT NULL,
    atualizado_em    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_material PRIMARY KEY (id),
    CONSTRAINT uq_material_codigo UNIQUE (codigo),
    CONSTRAINT ck_material_tipo CHECK (tipo IN ('PRODUTO_ACABADO', 'SEMIACABADO', 'MATERIA_PRIMA'))
);

COMMENT ON TABLE material IS 'Insumos e produtos gerenciados pelo PCP (PA, SA, MP).';
COMMENT ON COLUMN material.codigo IS 'Código único do material, normalizado em maiúsculas (ex: MAT-001).';
COMMENT ON COLUMN material.tipo IS 'Classifica o papel do material: PRODUTO_ACABADO, SEMIACABADO ou MATERIA_PRIMA.';