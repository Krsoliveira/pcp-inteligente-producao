-- ADR-0019: cada material tem uma lista técnica com várias versões. O lista_id identifica a lista
-- (compartilhado por todas as versões do mesmo material); lista_tecnica.id segue sendo o da versão.
--
-- Rollback: migrações são imutáveis; para desfazer, criar uma migração nova que remova o índice
-- e a coluna lista_id. Fazer backup antes de aplicar em ambiente com dados reais.

ALTER TABLE lista_tecnica ADD COLUMN lista_id UUID;

-- Um identificador por material nos dados existentes (gen_random_uuid() é nativa no PostgreSQL 13+).
UPDATE lista_tecnica lt
   SET lista_id = ids.lista_id
  FROM (SELECT material_id, gen_random_uuid() AS lista_id
          FROM lista_tecnica
         GROUP BY material_id) ids
 WHERE lt.material_id = ids.material_id;

ALTER TABLE lista_tecnica ALTER COLUMN lista_id SET NOT NULL;

CREATE INDEX idx_lista_tecnica_lista ON lista_tecnica (lista_id);

COMMENT ON COLUMN lista_tecnica.lista_id IS 'Identifica a lista técnica do material; todas as versões do mesmo material compartilham este valor.';
