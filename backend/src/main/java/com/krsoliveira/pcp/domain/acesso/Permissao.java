package com.krsoliveira.pcp.domain.acesso;

/**
 * Catálogo das capacidades que um perfil de acesso pode ter (ADR-0020). É definido no código:
 * adicionar uma permissão é uma mudança de código; marcar ou desmarcar em um perfil é dado.
 *
 * O nome do enum é o identificador gravado no banco e usado como autoridade do Spring Security.
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public enum Permissao {

    DASHBOARD_VISUALIZAR("Painel", "Ver o painel de produção"),

    MATERIAL_VISUALIZAR("Materiais", "Consultar materiais"),
    MATERIAL_CADASTRAR("Materiais", "Cadastrar materiais"),

    LISTA_VISUALIZAR_ATIVAS("Listas técnicas", "Ver apenas as versões ativas"),
    LISTA_VISUALIZAR_TODAS("Listas técnicas", "Ver todas as versões (em revisão, ativas e obsoletas)"),
    LISTA_COMPARAR("Listas técnicas", "Comparar duas versões"),
    LISTA_CADASTRAR("Listas técnicas", "Cadastrar lista e criar nova versão"),
    LISTA_ATIVAR("Listas técnicas", "Ativar uma versão"),
    LISTA_RETIRAR_DE_USO("Listas técnicas", "Retirar uma versão de uso"),

    ORDEM_VISUALIZAR("Ordens de produção", "Consultar ordens"),
    ORDEM_CRIAR("Ordens de produção", "Criar ordens"),
    ORDEM_ATUALIZAR_STATUS("Ordens de produção", "Liberar, iniciar e cancelar ordens"),
    ORDEM_CONCLUIR("Ordens de produção", "Concluir ordens (gera o lote)"),

    CONSUMO_VISUALIZAR("Consumos", "Consultar consumos de material"),
    CONSUMO_REGISTRAR("Consumos", "Registrar consumos de material"),
    CONSUMO_ADICIONAR_ITEM("Consumos", "Adicionar ao consumo um item que não está na lista técnica"),

    TIPO_ORDEM_VISUALIZAR("Tipos de ordem", "Consultar tipos de ordem"),
    TIPO_ORDEM_GERENCIAR("Tipos de ordem", "Criar e alterar tipos de ordem"),

    NOTA_FISCAL_VISUALIZAR("Notas fiscais", "Consultar notas fiscais de entrada"),
    NOTA_FISCAL_REGISTRAR("Notas fiscais", "Registrar entrada de notas fiscais"),

    ESTOQUE_VISUALIZAR("Estoque", "Consultar estoque, lotes e rastreabilidade"),

    AUDITORIA_VISUALIZAR("Auditoria", "Ver o histórico de alterações"),

    RELATORIO_CONSUMO_VISUALIZAR("Relatórios", "Ver o relatório de consumo de ordens"),

    ACESSO_VISUALIZAR("Acessos", "Ver perfis e usuários"),
    PERFIL_GERENCIAR("Acessos", "Criar, alterar e excluir perfis"),
    USUARIO_GERENCIAR("Acessos", "Criar usuários e atribuir perfis");

    private final String area;
    private final String descricao;

    Permissao(String area, String descricao) {
        this.area = area;
        this.descricao = descricao;
    }

    /** Agrupamento mostrado na tela de perfis. */
    public String getArea() { return area; }

    public String getDescricao() { return descricao; }
}
