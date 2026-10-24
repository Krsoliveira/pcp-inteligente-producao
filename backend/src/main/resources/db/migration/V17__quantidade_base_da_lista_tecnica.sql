-- ADR-0027: quantidade base da lista técnica.
-- A lista passa a dizer "para produzir N unidades (ou kg) do material, usa-se estes componentes". O consumo planejado
-- de uma ordem vira quantidade do item ÷ base × quantidade da ordem. O padrão é 1: as listas existentes continuam
-- significando "por 1 unidade", e nada muda no que já foi calculado.

ALTER TABLE lista_tecnica ADD COLUMN quantidade_base NUMERIC(12, 4) NOT NULL DEFAULT 1;
ALTER TABLE lista_tecnica ADD CONSTRAINT ck_lista_tecnica_quantidade_base CHECK (quantidade_base > 0);

COMMENT ON COLUMN lista_tecnica.quantidade_base IS 'Quantidade do material produzido a que se referem as quantidades dos componentes (unidade do material produzido). Padrão 1.';
