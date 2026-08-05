-- V3: adiciona centro de trabalho à ordem de produção.
-- Centro de trabalho identifica qual máquina/linha/estação executa a ordem —
-- campo fundamental para análise de gargalos e recomendações de IA (Fase 5).
--
-- NOT NULL with DEFAULT durante a alteração: o banco exige um valor para as
-- linhas existentes. Em ambiente novo (sem dados anteriores) o DEFAULT nunca
-- aparece; em ambiente de dev com dados legados garante consistência.
-- O DEFAULT é removido logo em seguida — novas inserções DEVEM informar o campo.

ALTER TABLE ordem_producao
    ADD COLUMN centro_de_trabalho VARCHAR(60) NOT NULL DEFAULT 'Não informado';

ALTER TABLE ordem_producao
    ALTER COLUMN centro_de_trabalho DROP DEFAULT;

-- O dashboard e o módulo de IA filtrarão ordens por centro com frequência.
CREATE INDEX idx_ordem_producao_centro ON ordem_producao (centro_de_trabalho);