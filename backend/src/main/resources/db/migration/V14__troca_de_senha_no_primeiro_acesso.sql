-- ADR-0021: troca de senha no primeiro acesso.
--
-- deve_trocar_senha: o usuário precisa definir uma senha nova antes de usar o sistema. Fica ligada para
-- quem é criado por um gestor e para quem teve a senha redefinida por um gestor (a senha provisória é
-- conhecida por quem a definiu); desligada no autocadastro e no administrador inicial. Os usuários que já
-- existem ficam com FALSE (escolheram a senha, ou ela vem do ambiente).
--
-- Rollback: migrações são imutáveis; para desfazer, criar uma migração nova que remova a coluna.

ALTER TABLE usuario ADD COLUMN deve_trocar_senha BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN usuario.deve_trocar_senha IS 'Enquanto TRUE, o servidor só aceita consultar quem sou eu e trocar a senha (ADR-0021).';
