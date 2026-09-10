-- V7: Adaptar ordem_producao para referenciar Material e Lista Técnica (Fase 5a)
--
-- Remove o campo produto (VARCHAR solto) e adiciona:
--   material_id   → o material a produzir
--   lista_tecnica_id → a versão de BOM usada para esta ordem
--
-- ATENÇÃO (DEV): a tabela é truncada antes da alteração porque os dados
-- de seed (CSV) usam o campo produto e são incompatíveis com o novo modelo.
-- O dataset sintético para o novo modelo (Material + BOM + Lote + Consumo)
-- será gerado por script Python separado (ADR-0007).
--
-- Em produção, uma migração deste tipo exigiria: backup, script de conversão
-- de dados e janela de manutenção — documentado aqui para registro.

TRUNCATE TABLE ordem_producao;

ALTER TABLE ordem_producao
    DROP COLUMN produto;

ALTER TABLE ordem_producao
    ADD COLUMN material_id       UUID NOT NULL,
    ADD COLUMN lista_tecnica_id  UUID NOT NULL;

ALTER TABLE ordem_producao
    ADD CONSTRAINT fk_ordem_producao_material
        FOREIGN KEY (material_id) REFERENCES material (id),
    ADD CONSTRAINT fk_ordem_producao_lista_tecnica
        FOREIGN KEY (lista_tecnica_id) REFERENCES lista_tecnica (id);

CREATE INDEX idx_ordem_producao_material ON ordem_producao (material_id);

COMMENT ON COLUMN ordem_producao.material_id IS 'Material a ser produzido (substitui campo produto VARCHAR).';
COMMENT ON COLUMN ordem_producao.lista_tecnica_id IS 'Versão da BOM vigente no momento da criação da ordem.';
