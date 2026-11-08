-- V20: número de versão nos registros editáveis por tela, para recusar a edição feita sobre dados antigos (ADR-0034).
--
-- Sem ele, duas pessoas que abrem o mesmo perfil (ou tipo de ordem) e salvam uma depois da outra perdem a alteração
-- da primeira sem aviso. A tela devolve a versão que leu; se o registro já mudou, a API responde 409.
--
-- Só acrescenta colunas: os registros existentes começam na versão 0, e o código anterior continua funcionando com
-- elas no banco (rollback é só voltar o código).

ALTER TABLE perfil_acesso ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tipo_ordem    ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
