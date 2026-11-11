-- ADR-0037: a unidade de medida passa a ser guardada em maiúsculas e sem espaços nas pontas ("kg" -> "KG").
-- O domínio normaliza as novas; aqui entram as que já existem, nas quatro tabelas que a guardam.
--
-- Rollback: a caixa original não se recupera por SQL (UPPER perde a informação), mas ela não carrega significado:
-- a versão anterior da aplicação lê "KG" normalmente. Se for preciso voltar ao texto original, restaurar o backup
-- feito antes do deploy. Fazer backup antes de aplicar em ambiente com dados reais.

UPDATE material
   SET unidade_de_medida = UPPER(TRIM(unidade_de_medida))
 WHERE unidade_de_medida <> UPPER(TRIM(unidade_de_medida));

UPDATE item_lista_tecnica
   SET unidade_de_medida = UPPER(TRIM(unidade_de_medida))
 WHERE unidade_de_medida <> UPPER(TRIM(unidade_de_medida));

UPDATE consumo_material
   SET unidade_de_medida = UPPER(TRIM(unidade_de_medida))
 WHERE unidade_de_medida <> UPPER(TRIM(unidade_de_medida));

UPDATE lote
   SET unidade_de_medida = UPPER(TRIM(unidade_de_medida))
 WHERE unidade_de_medida <> UPPER(TRIM(unidade_de_medida));
