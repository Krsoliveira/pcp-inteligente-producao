-- V21: desativar um usuário sem excluí-lo (pendência de acessos; ADR-0020).
--
-- Usuário desativado não entra e perde a sessão na requisição seguinte, mas continua existindo: a trilha de auditoria
-- e as assinaturas dos registros ("criado por") seguem apontando para ele. Todos os usuários existentes ficam ativos.

ALTER TABLE usuario ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
