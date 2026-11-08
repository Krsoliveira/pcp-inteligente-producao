-- ADR-0036: número legível da lista técnica (LT-0001). Todas as versões de uma lista compartilham o número.
-- O banco guarda só o inteiro; o formato "LT-0001" é montado pela aplicação.
--
-- Rollback: migração aditiva (a coluna extra é ignorada pela versão anterior da aplicação). Com NOT NULL, a versão
-- anterior não consegue CRIAR lista nova; se isso for preciso, aplicar uma migração nova que faça
-- ALTER TABLE lista_tecnica ALTER COLUMN numero_lista DROP NOT NULL. Fazer backup antes de aplicar em produção.

CREATE SEQUENCE lista_tecnica_numero_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE lista_tecnica ADD COLUMN numero_lista BIGINT;

-- Um número por lista (lista_id), na ordem em que a primeira versão de cada lista foi criada.
UPDATE lista_tecnica lt
   SET numero_lista = numeradas.numero
  FROM (SELECT lista_id, ROW_NUMBER() OVER (ORDER BY MIN(criada_em), lista_id) AS numero
          FROM lista_tecnica
         GROUP BY lista_id) numeradas
 WHERE lt.lista_id = numeradas.lista_id;

-- A sequência segue a partir do maior número já atribuído (is_called = false quando não há lista alguma).
SELECT setval('lista_tecnica_numero_seq', COALESCE(MAX(numero_lista), 1), COUNT(*) > 0) FROM lista_tecnica;

ALTER TABLE lista_tecnica ALTER COLUMN numero_lista SET NOT NULL;

CREATE INDEX idx_lista_tecnica_numero ON lista_tecnica (numero_lista);

COMMENT ON COLUMN lista_tecnica.numero_lista IS 'Número legível da lista técnica (LT-0001); todas as versões do mesmo material compartilham este valor.';
