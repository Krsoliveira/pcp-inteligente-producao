-- V2: tabela de usuários para autenticação e autorização.
-- Cada usuário possui um perfil (PLANEJADOR ou GERENTE) que determina
-- o que ele pode fazer na plataforma PCP.

CREATE TABLE usuario (
    id          UUID         PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    senha_hash  VARCHAR(255) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL,
    criado_em   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_usuario_email  UNIQUE (email),
    CONSTRAINT chk_usuario_perfil CHECK (perfil IN ('PLANEJADOR', 'GERENTE'))
);