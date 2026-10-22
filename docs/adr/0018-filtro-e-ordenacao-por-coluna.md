# ADR-0018: Filtro e ordenação por coluna no cabeçalho (estilo planilha)

- **Status**: Aceito
- **Data**: 2026-10-22
- **Substitui em parte**: ADR-0016 (o "subfiltro" de texto livre sobre a página)

## Contexto

O ADR-0016 deu às listagens uma consulta em duas etapas: filtros no servidor e um subfiltro, no
navegador, que era um único texto livre sobre as colunas visíveis. No uso, o texto livre não se
mostrou intuitivo: o usuário pensa em colunas ("só este fornecedor", "do maior para o menor"), como
numa planilha, e não numa busca genérica. O primeiro filtro, por sua vez, repetia no servidor o que
as colunas já permitem (ex.: busca por nota, fornecedor e lote nas Notas Fiscais).

## Decisão

1. **Primeiro filtro (servidor)**: poucos critérios simples, que reduzem o volume vindo do banco
   (ex.: período de recebimento nas Notas Fiscais; tipo, status, "só com saldo" nas demais telas).
   A busca por texto só permanece onde ainda é o único meio de achar um item entre muitas páginas.
2. **Segundo filtro (navegador)**: cada coluna tem, no título, um ícone para **ordenar** (crescente,
   decrescente, ordem original) e um funil para **filtrar** a coluna (painel com campo de texto e
   "Limpar"). Só atua sobre as linhas da página já consultada; não faz nova requisição. Vale também
   para a tabela dos "10 mais recentes".
3. Implementação única e reutilizável: `useTabelaInterativa`, `CelulaDeCabecalho` e `ResumoDaTabela`
   (`frontend/src/components/consulta/TabelaInterativa.tsx`). Cada tabela exporta suas colunas
   (`valor` para comparar; `texto` para o que aparece na célula e onde o filtro procura).
4. Ordenação: comparação natural em pt-BR (`OP-2` antes de `OP-10`), números pelo valor, vazios sempre
   no fim.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Manter o texto livre | Pouco intuitivo, segundo o usuário; não permite filtrar por coluna nem ordenar |
| Caixa "Ordenar por" fora da tabela | Funciona, mas distante da coluna; a planilha é a referência do usuário |
| Linha de campos de filtro sob o cabeçalho | Ocupa espaço o tempo todo e polui a tabela; o funil sob demanda é mais limpo |
| Ordenar e filtrar no servidor | Seria a forma correta de classificar *todo* o resultado, mas exige parâmetros e índices novos; fica como evolução se necessário |

## Consequências

- **Positivas**: padrão único nas 5 telas; nenhuma consulta extra; testes do comportamento no hook.
- **Negativas / riscos**: ordenar e filtrar só enxerga a página atual (25 linhas por padrão). Quem
  precisa classificar todo o resultado ainda depende do servidor. A faixa acima da tabela deixa claro
  que o escopo é "esta página".
