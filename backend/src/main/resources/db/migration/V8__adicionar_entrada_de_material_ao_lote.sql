-- V8: Entrada de material (compra de matéria-prima) gera lote rastreável.
--
-- Um lote passa a ter duas origens possíveis:
--   * produção → ordem_producao_id preenchido
--   * compra   → fornecedor + nota_fiscal preenchidos
-- A constraint garante que todo lote tenha exatamente uma origem.

ALTER TABLE lote
    ADD COLUMN fornecedor  VARCHAR(150),
    ADD COLUMN nota_fiscal VARCHAR(44);

ALTER TABLE lote
    ADD CONSTRAINT ck_lote_origem CHECK (
        (ordem_producao_id IS NOT NULL AND fornecedor IS NULL AND nota_fiscal IS NULL)
        OR (ordem_producao_id IS NULL AND fornecedor IS NOT NULL AND nota_fiscal IS NOT NULL)
    );

-- Mesma nota fiscal do mesmo fornecedor não pode dar entrada duas vezes no mesmo material.
CREATE UNIQUE INDEX uq_lote_entrada_compra
    ON lote (material_id, fornecedor, nota_fiscal)
    WHERE nota_fiscal IS NOT NULL;

COMMENT ON COLUMN lote.fornecedor IS 'Fornecedor da matéria-prima (apenas lotes de compra).';
COMMENT ON COLUMN lote.nota_fiscal IS 'Número ou chave (44 dígitos) da NF de entrada (apenas lotes de compra).';
