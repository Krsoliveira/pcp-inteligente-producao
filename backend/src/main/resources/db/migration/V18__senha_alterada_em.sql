-- Quando a senha do usuário mudou pela última vez. Token emitido antes disso deixa de valer: quem troca a senha porque
-- suspeita de um vazamento não pode continuar com o token antigo em circulação até expirar.
--
-- Nulo para quem nunca trocou desde esta migração: os tokens atuais continuam valendo até a expiração normal.
--
-- Rollback: migrações são imutáveis; para desfazer, criar uma migração nova que remova a coluna. Reverter só o código
-- também é seguro, porque a coluna aceita nulo e nada mais depende dela.

ALTER TABLE usuario ADD COLUMN senha_alterada_em TIMESTAMPTZ;

COMMENT ON COLUMN usuario.senha_alterada_em IS 'Última mudança de senha; tokens emitidos antes deixam de valer. Nulo: nunca mudou desde a V18.';
