# Design system

Padrão visual e de interação do frontend (React 19 + MUI 9). Vale para toda tela, componente, tabela, formulário e gráfico novos.

**Fonte da verdade dos tokens:** [`frontend/src/theme.ts`](../frontend/src/theme.ts). As tabelas abaixo resumem o tema; se divergirem, vale o arquivo, e este documento deve ser corrigido. Antes de criar componente ou estilo, procure o que já existe em `components/`, `utils/` e `hooks/`.

## Identidade visual

Software corporativo para ambiente industrial: o visual transmite confiança, precisão e seriedade.

- Clareza acima de decoração. Cada elemento tem função.
- Densidade média: dados visíveis sem rolar, sem espaço sobrando.
- Hierarquia clara: o usuário sabe onde está e o que fazer a seguir.
- Cor por token semântico (`primary.main`, `text.secondary`), não por valor.

## Paleta (tema claro, o único hoje)

| Token | Valor | Uso |
|---|---|---|
| `primary.main` / `.dark` / `.light` | `#4F46E5` / `#3730A3` / `#818CF8` | Ação principal, link, item ativo / hover / destaque suave |
| `secondary.main` | `#0F766E` | Ação secundária |
| `success.main` | `#047857` | Positivo (concluída, no prazo), como texto ou preenchimento |
| `warning.main` | `#B45309` | Atenção (desvio moderado), como texto ou preenchimento |
| `error.main` | `#DC2626` | Erro, atraso, crítico |
| `info.main` | `#2563EB` | Informação neutra |
| `background.default` / `.paper` | `#F8FAFC` / `#FFFFFF` | Fundo da página / cards e diálogos |
| `text.primary` / `.secondary` | `#0F172A` / `#64748B` | Texto / rótulos, metadados |
| `divider` | `rgba(0,0,0,0.07)` | Divisores e bordas |

Todo tom da paleta usado como texto tem contraste de pelo menos 4,5:1 sobre os dois fundos; `theme.test.ts` reprova o que ficar abaixo. **Sobre fundo tingido, o contraste cai:** `warning.main` em chip com fundo da própria cor dá 4,49:1. Nesses casos, use o tom escuro: `warning.dark` do tema, ou `#92400E` em constante.

`text.disabled` é só para controle realmente desabilitado e ícone decorativo. Traço de valor vazio e texto informativo usam `text.secondary`.

**Cor literal (`'#...'`) só em constante nomeada no topo do arquivo, mapeando significado → cor**, e só onde o tema não alcança:

- menu lateral (`Layout.tsx`);
- status (`StatusBadge.tsx`), tipo de material (`MaterialTable.tsx`) e paleta de escolha dos tipos de ordem (`TiposOrdemPage.tsx`);
- gráficos ECharts, que não leem o tema.

Barra de gráfico e ponto de status não são texto, então podem usar os tons vivos (`#059669`, `#D97706`).

## Tipografia

Família: `"Inter", "Roboto", "Helvetica", "Arial", sans-serif`.

| Variante | Peso | Elemento | Uso |
|---|---|---|---|
| `h5` | 700 | `h1` (via `PageHeader`) | Título da página |
| `h4` | 700 | `p` (via `KpiCard`) | Número de KPI |
| `h6`, `subtitle1`, `subtitle2` | 600 | `h2` (mapeado no tema) | Título de card ou seção |
| `body1`, `body2` | 400 | `p` | Conteúdo, descrição |
| `caption` | 400 | `span` | Metadado, data, rodapé |

- Toda tela tem um único `h1`, e os níveis não pulam. Texto que usa uma variante de título sem ser título declara o elemento: `component="div"`.
- Cabeçalho de tabela já vem do tema. Não repita no componente.
- Colunas numéricas (`align="right"`) têm dígitos tabulares pelo tema.
- Mínimo de `0.65rem` no conteúdo; abaixo disso, só os subtítulos do menu lateral (`0.62rem`).

## Textos

- **Caixa de frase em tudo:** "Nova ordem", "Alterar status", "Listas técnicas", "Em produção".
- Botão com verbo e objeto: "Registrar consumo", não "OK".
- Contagens por `contagem(total, 'material', 'materiais')`, que acerta o singular.
- Rótulo de status de ordem na tela: `rotuloDoStatusDeOrdem`, de `StatusBadge.tsx`.

## Espaçamento e layout

- Grade de 8 px (`1` = 8 px). Padding de página `p: { xs: 2, sm: 3 }`; gap entre KPIs `2`.
- `borderRadius` do tema = 8 px; até 12 px em card grande; nunca acima de 16 px.
- Menu lateral: 248 px, fundo `#0F172A`, item ativo `#4F46E5` com texto branco, inativo `rgba(255,255,255,0.65)`, subtítulos `rgba(255,255,255,0.6)`, divisores `rgba(255,255,255,0.08)`.
- Tabela: célula com 12 px nas laterais, para caber 8 colunas em 1440 px. Ao acrescentar coluna, confira essa largura.

## Componentes e utilitários (reutilize)

| Componente / utilitário | Para quê |
|---|---|
| `PageHeader` (`titulo`, `subtitulo`, `breadcrumbs`, `action`) | Título de toda página, com navegação e ação principal |
| `EmptyState` (`titulo`, `descricao`, ícone) | Lista ou busca sem resultado |
| `StatusOrdemBadge`, `StatusListaBadge`, `StatusLoteBadge`, `TipoOrdemBadge` | Status: ponto colorido + rótulo |
| `KpiCard`, `InfoRow`, `PaginacaoTabela`, `HistoricoAuditoria` | KPI, par rótulo/valor, paginação, trilha de auditoria |
| `LinkDaLinha` | Identificador de linha clicável: abre o registro por teclado e em outra aba |
| `DadosDoGrafico` | Números de um gráfico em tabela, com link para a lista de cada barra |
| `useTabelaInterativa`, `CelulaDeCabecalho`, `ResumoDaTabela` | Ordenar e filtrar por coluna |
| `SePermitido`, `ExigirPermissao`, `SemAcesso` | Esconder ou barrar por permissão (a barreira real é o servidor) |
| `utils/formatacao.ts` | `formatarData`, `formatarDataHora`, `formatarQuantidade`, `formatarCodigoMaterial`, `contagem`, `hojeIso` |
| `utils/mensagensDeErro.ts` | `mensagemDeErroDaApi`, `mensagemDeErroAoCarregar`, espera do 429 |
| `hooks/useParametrosDaUrl`, `usePaginacaoLocal`, `usePermissoes` | Filtros na URL, paginação local, permissões |

Nunca formate data, número ou erro à mão na tela:

- `toLocaleString` sem opções limita a 3 casas, e o sistema guarda 4: use `formatarQuantidade`.
- `toISOString` dá o dia em UTC, já o seguinte à noite no Brasil: use `hojeIso`.

## Superfícies

- Card e Paper sem sombra (`elevation` 0, padrão no tema) e com borda `divider`. Sombra só em diálogo, menu e popover.
- Seção de página de detalhe: `<Paper variant="outlined" sx={{ p: 2.5 }}>` ou `Card` do tema.

## Tabelas

- Cabeçalho, borda de célula e hover vêm do tema. Não sobrescreva no componente.
- Filtros seguem o [ADR-0018](adr/0018-filtro-e-ordenacao-por-coluna.md): o primeiro filtro simples vai ao banco; o segundo fica por coluna, no cabeçalho.
- **Linha clicável sempre tem `LinkDaLinha` no identificador.** O clique na linha é só um atalho para o mouse.
- Descrição longa quebra dentro da coluna (`overflowWrap: 'anywhere'` com `LARGURA_MINIMA_DA_DESCRICAO`).
- Ícone de ação em tabela tem alvo de pelo menos 24 x 24 px.

## Botões

- **Primário:** `variant="contained"` (sem sombra, peso 600 e sem maiúsculas, pelo tema).
- **Secundário:** `variant="outlined"`.
- **Destrutivo:** o gatilho na tela é `outlined` ou `IconButton` com `color="error"`; o `contained` vermelho só no botão de confirmação do diálogo.
- `size="small"` em toolbars e tabelas; `medium` em formulário isolado.

## Status

Ponto na cor plena + fundo `alpha(cor, 0.10–0.12)` + texto num tom **mais escuro** da mesma cor, como em `StatusBadge.tsx`. Nunca fundo sólido. O mesmo status tem a mesma cor em selo, gráfico e filtro.

| Status | Ponto |
|---|---|
| Planejada | `#94A3B8` |
| Liberada | `#2563EB` |
| Em produção | `#D97706` |
| Concluída | `#059669` |
| Cancelada | `#DC2626` |

A cor do tipo de ordem é escolhida pelo usuário e nem toda é legível como texto: ela fica no fundo e na borda, com texto em `text.primary`.

## Estados de tela (obrigatórios)

Toda tela que busca dados trata os quatro estados:

```tsx
const falhouSemDados = isError && !data

{isError && <Alert severity="error">{mensagemDeErroAoCarregar(error, 'Não foi possível carregar as ordens.')}</Alert>}
{!falhouSemDados && (
  <Card>
    {isLoading ? <Skeleton … /> : itens.length === 0 ? <EmptyState … /> : <Tabela … />}
  </Card>
)}
```

- **Falha ao carregar não é lista vazia:** sem dados, só o alerta aparece, sem "0 cadastrados" nem "Nenhum cadastrado". Com dados anteriores, eles continuam visíveis.
- Em listagem com filtro, a mensagem vazia diz que **o filtro** não encontrou nada e oferece limpar.

## Formulários

- `size="small"` vem do tema; o rótulo flutua dentro do contorno.
- Erro de campo inline (`helperText`), nunca em toast. Erro da API no topo do diálogo, com `mensagemDeErroDaApi`. A validação do cliente ajuda o usuário; quem decide é o servidor.
- Submit: `disabled={isPending}` e texto no gerúndio durante o envio ("Salvando…"). Nunca permita envio duplo.
- Texto de ajuda que não pode deslocar o layout (tabela de lançamento): `formHelperText` com `position: 'absolute'`, como em `LancamentoDeConsumoPage`.

## Ícones

Só `@mui/icons-material`. Navegação e ações de linha em variante `Outlined`; ícone de status ou alerta pode ser cheio. `fontSize="small"` em listas e tabelas. No MUI 9, alguns nomes mudaram (`AddCircleOutline` → `AddCircleOutlineOutlined`): confira o desenho, não só o nome.

## Regras do MUI 9

- Props de estilo vão no `sx` (`fontWeight`, `color`, `mt`…), não como prop do componente.
- `color="primary"` funciona; `color={expressão}` com `text.secondary` ou `error.main` **não pinta**. Cor calculada vai em `sx={{ color: … }}`.
- `inputProps` → `slotProps={{ htmlInput: … }}`; `primaryTypographyProps` → `slotProps={{ primary: { sx: … } }}`; `Grid` com `size={…}`.
- No `sx`, `width: 1` significa 100%. Para 1 pixel, escreva `'1px'`.
- `Switch` tem `role="switch"` (nos testes, `getByRole('switch', …)`).

## Acessibilidade (WCAG 2.2 AA)

- **Teclado:** tudo que abre ou executa alcança o foco. O foco visível vem do tema (`MuiButtonBase`); sobre o menu escuro, o contorno é branco. Navegar é link; executar é botão.
- **Nome:** `IconButton` sem texto tem `aria-label` com o objeto ("Excluir perfil Operador"). Tooltip complementa, não substitui. Se houver `aria-label`, ele contém o texto visível.
- **Cor nunca é a única informação:** status tem rótulo; desvio tem número.
- **Gráfico:** `role="img"` com `aria-label` trazendo os números, mais `DadosDoGrafico`.
- **Mudança de resultado** é anunciada com `role="status"` (já em `ResumoDaTabela`).

## Conferir no navegador

Testes em jsdom não veem CSS. Mudança visual relevante (coluna nova, diálogo, cor) é conferida no Chromium em 1440 px e numa largura menor, com axe. Em mudança ampla (biblioteca, tema), compare capturas antes e depois.

## O que evitar

- Gradiente ou animação decorativa; transição acima de 200 ms.
- Sombra em card de conteúdo.
- Cor literal fora de constante nomeada; tom que não existe no tema.
- Fonte ou peso fora do tema.
- Estilo que o tema já aplica, repetido no componente.
- Componente novo que duplica um da tabela "reutilize".
