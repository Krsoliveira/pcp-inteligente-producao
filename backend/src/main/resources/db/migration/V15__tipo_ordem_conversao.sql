-- ADR-0024: produto acabado só é consumido em ordens do tipo Conversão (converte acabado em semiacabado).
--   tipo_ordem.conversao : marca o tipo de sistema que pode consumir produto acabado.
-- Os tipos já cadastrados ficam como estão (conversao = FALSE). O tipo Conversão é criado aqui; se já existir um
-- tipo com esse nome, ele passa a ser o tipo Conversão.

ALTER TABLE tipo_ordem ADD COLUMN conversao BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN tipo_ordem.conversao IS 'Tipo de sistema Conversão: a única categoria de ordem que pode consumir produto acabado (e só produz semiacabado).';

CREATE UNIQUE INDEX uq_tipo_ordem_conversao ON tipo_ordem (conversao) WHERE conversao;

INSERT INTO tipo_ordem (id, nome, descricao, cor, criado_em, atualizado_em, criado_por, atualizado_por, conversao)
VALUES ('00000000-0000-0000-0000-0000000000c0', 'Conversão',
        'Converte produto acabado em semiacabado. É o único tipo de ordem que pode consumir produto acabado.',
        '#6a1b9a', NOW(), NOW(), 'sistema:migracao-v15', 'sistema:migracao-v15', TRUE)
ON CONFLICT (nome) DO UPDATE SET conversao = TRUE;
