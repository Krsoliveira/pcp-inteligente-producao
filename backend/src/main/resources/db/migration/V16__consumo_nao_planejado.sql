-- ADR-0025: itens adicionados ao consumo fora da lista técnica.
-- Uma linha de consumo adicionada pelo usuário nasce com quantidade planejada ZERO (não planejada); as linhas
-- projetadas da lista continuam sempre com quantidade planejada maior que zero (regra do domínio).

ALTER TABLE consumo_material DROP CONSTRAINT ck_consumo_planejada;
ALTER TABLE consumo_material ADD CONSTRAINT ck_consumo_planejada CHECK (quantidade_planejada >= 0);

COMMENT ON COLUMN consumo_material.quantidade_planejada IS 'Zero = item adicionado pelo usuário ao lançar o consumo, fora da lista técnica.';
