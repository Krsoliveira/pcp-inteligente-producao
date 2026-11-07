-- V19: o perfil Visualizador passa a ver o relatório de consumo de ordens (ADR-0020, atualização de 2026-11-07).
--
-- A permissão RELATORIO_CONSUMO_VISUALIZAR surgiu depois da V13, e só o Administrador, que tem todas, a recebeu.
-- Concede só ela: o Visualizador continua sem CONSUMO_VISUALIZAR, e vê os totais por componente das ordens concluídas,
-- não o consumo de cada ordem.
--
-- O perfil é o de id fixo da V13, mesmo que tenha sido renomeado. Se ele já tem a permissão (um gestor a concedeu)
-- ou não existe, nada muda. A concessão, a assinatura do perfil e o evento na trilha de auditoria acontecem juntos,
-- na mesma instrução, como se a mudança tivesse sido feita pela tela.

WITH concedida AS (
    INSERT INTO perfil_acesso_permissao (perfil_id, permissao)
    SELECT id, 'RELATORIO_CONSUMO_VISUALIZAR'
      FROM perfil_acesso
     WHERE id = '00000000-0000-0000-0000-000000000002'
    ON CONFLICT (perfil_id, permissao) DO NOTHING
    RETURNING perfil_id
), assinado AS (
    UPDATE perfil_acesso p
       SET atualizado_em = NOW(), atualizado_por = 'sistema:migracao-v19'
      FROM concedida c
     WHERE p.id = c.perfil_id
    RETURNING p.id, p.nome
)
INSERT INTO evento_auditoria (id, tipo_entidade, entidade_id, referencia, acao, usuario, ocorrido_em, detalhes)
SELECT gen_random_uuid(), 'PERFIL_ACESSO', id, nome, 'ALTERADO', 'sistema:migracao-v19', NOW(),
       jsonb_build_object('perfil', nome,
                          'permissoesAdicionadas', 'RELATORIO_CONSUMO_VISUALIZAR',
                          'motivo', 'Relatório de consumo concedido ao perfil Visualizador (ADR-0020)')
  FROM assinado;
