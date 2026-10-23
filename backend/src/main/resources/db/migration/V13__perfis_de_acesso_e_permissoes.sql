-- ADR-0020: controle de acesso por perfis de permissões.
--
--   perfil_acesso            : perfil com id, nome único, descrição e duas marcas de sistema
--   perfil_acesso_permissao  : permissões (nome do enum Permissao) de cada perfil
--   usuario.perfil_id        : substitui o texto usuario.perfil (PLANEJADOR/GERENTE)
--
-- ATENÇÃO: esta migração EXCLUI TODOS OS USUÁRIOS existentes (decisão do usuário: recomeçar já com a
-- regra nova). Quem entra depois é o administrador inicial (ADMIN_EMAIL/ADMIN_NOME/ADMIN_SENHA no
-- ambiente) e o que ele criar. Os registros de auditoria guardam o e-mail como texto, sem chave
-- estrangeira, e não são afetados.
--
-- Rollback: migrações são imutáveis; para desfazer, criar uma migração nova que recrie
-- usuario.perfil e remova as tabelas abaixo. Os usuários apagados não voltam: fazer backup antes de
-- aplicar em ambiente com dados reais.

CREATE TABLE perfil_acesso (
    id                      UUID          NOT NULL,
    nome                    VARCHAR(60)   NOT NULL,
    descricao               VARCHAR(200)  NOT NULL DEFAULT '',
    administrador           BOOLEAN       NOT NULL DEFAULT FALSE,
    padrao_do_autocadastro  BOOLEAN       NOT NULL DEFAULT FALSE,
    criado_em               TIMESTAMPTZ   NOT NULL,
    atualizado_em           TIMESTAMPTZ   NOT NULL,
    criado_por              VARCHAR(150)  NOT NULL,
    atualizado_por          VARCHAR(150)  NOT NULL,

    CONSTRAINT pk_perfil_acesso PRIMARY KEY (id)
);

-- Nome único sem diferenciar maiúsculas e minúsculas.
CREATE UNIQUE INDEX uq_perfil_acesso_nome ON perfil_acesso (LOWER(nome));
-- No máximo um administrador e um perfil padrão do autocadastro.
CREATE UNIQUE INDEX uq_perfil_acesso_administrador ON perfil_acesso (administrador) WHERE administrador;
CREATE UNIQUE INDEX uq_perfil_acesso_padrao ON perfil_acesso (padrao_do_autocadastro) WHERE padrao_do_autocadastro;

CREATE TABLE perfil_acesso_permissao (
    perfil_id  UUID         NOT NULL,
    permissao  VARCHAR(40)  NOT NULL,

    CONSTRAINT pk_perfil_acesso_permissao PRIMARY KEY (perfil_id, permissao),
    CONSTRAINT fk_perfil_acesso_permissao_perfil FOREIGN KEY (perfil_id)
        REFERENCES perfil_acesso (id) ON DELETE CASCADE
);

COMMENT ON TABLE perfil_acesso IS 'Perfil de acesso: conjunto de permissões atribuído a usuários (ADR-0020).';
COMMENT ON COLUMN perfil_acesso.administrador IS 'Perfil de sistema com todas as permissões, sempre; não pode ser editado nem excluído.';
COMMENT ON COLUMN perfil_acesso.padrao_do_autocadastro IS 'Perfil que o autocadastro público recebe; permissões editáveis, não pode ser excluído.';

-- Perfis de sistema, com ids fixos para os testes e a documentação.
INSERT INTO perfil_acesso (id, nome, descricao, administrador, padrao_do_autocadastro, criado_em, atualizado_em, criado_por, atualizado_por)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'Administrador',
   'Todas as permissões, sempre. Cria os demais perfis e usuários.', TRUE, FALSE,
   NOW(), NOW(), 'sistema:migracao-v13', 'sistema:migracao-v13'),
  ('00000000-0000-0000-0000-000000000002', 'Visualizador',
   'Só consulta. É o perfil de quem se cadastra sozinho, até um gerente atribuir outro.', FALSE, TRUE,
   NOW(), NOW(), 'sistema:migracao-v13', 'sistema:migracao-v13');

INSERT INTO perfil_acesso_permissao (perfil_id, permissao) VALUES
  ('00000000-0000-0000-0000-000000000002', 'DASHBOARD_VISUALIZAR'),
  ('00000000-0000-0000-0000-000000000002', 'MATERIAL_VISUALIZAR'),
  ('00000000-0000-0000-0000-000000000002', 'LISTA_VISUALIZAR_ATIVAS'),
  ('00000000-0000-0000-0000-000000000002', 'ORDEM_VISUALIZAR'),
  ('00000000-0000-0000-0000-000000000002', 'ESTOQUE_VISUALIZAR'),
  ('00000000-0000-0000-0000-000000000002', 'NOTA_FISCAL_VISUALIZAR');

-- Usuários: recomeço (ver aviso no topo), depois troca do texto pelo vínculo ao perfil.
DELETE FROM usuario;

ALTER TABLE usuario DROP CONSTRAINT chk_usuario_perfil;
ALTER TABLE usuario DROP COLUMN perfil;
ALTER TABLE usuario ADD COLUMN perfil_id UUID NOT NULL;
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_perfil FOREIGN KEY (perfil_id) REFERENCES perfil_acesso (id);
CREATE INDEX idx_usuario_perfil ON usuario (perfil_id);
