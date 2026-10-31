# ADR-0029: Migração do Material UI 6 para o 9

- **Status**: Aceito
- **Data**: 2026-10-31

## Contexto

O frontend usa o Material UI 6 (`@mui/material` e `@mui/icons-material` 6.5). A linha atual é a 9 (9.5.0, de
09/10/2026); a 8 não existiu. O Dependabot propôs a 9 (#78, #90), mas o PR falhava no CI: os ícones 9 exigem o
`@mui/material` 9, e a migração tem mudanças que quebram. Com React 19, Vite 8 e TypeScript 7 já atualizados
(#94–#96), o MUI é a última dependência principal do frontend numa linha antiga.

O levantamento no código (`frontend/src`) mostrou o que é afetado:

| Mudança | Versão | Onde aparece no projeto |
|---|---|---|
| `Grid2` passa a se chamar `Grid` | 7 | 13 arquivos (todos já usam `size`, o formato novo) |
| Props de sistema removidas de `Typography`/`Box`/`Stack`/`Link` (viram `sx`) | 9 | `fontWeight=` em 53 lugares, 31 arquivos |
| `inputProps` do `TextField` removido (vira `slotProps.htmlInput`) | 9 | 11 lugares, 5 arquivos |
| `primaryTypographyProps` do `ListItemText` removido (vira `slotProps.primary`) | 9 | 3 lugares, menu lateral |
| Ícones `*Outline` removidos (ficam os `*Outlined`) | 9 | `AddCircleOutline`, `CheckCircleOutline`, `DeleteOutline` |
| `ButtonBase` pede `nativeButton` quando o elemento muda | 9 | 3 `Button` com `component={RouterLink}` |
| `ListItemIcon` passa de 56 para 36 px de largura mínima | 9 | menu lateral (já ajusta a largura por `sx`) |
| `TextField select` renderiza o rótulo como `<div>` | 9 | selects dos diálogos e filtros (afeta testes por rótulo) |
| Navegadores mínimos: Chrome/Edge 117/121, Firefox 121, Safari 17 | 9 | todos os usuários |

Não usamos nada do resto da lista de remoções: Grid legado, `@mui/lab`, importações profundas, `Hidden`,
`InputProps`, `PaperProps`, `TransitionProps`, `components`/`componentsProps`, `disableEscapeKeyDown`, `Tabs`,
`Stepper`, `Slider`, `Autocomplete`, `TablePagination` com props antigas.

## Decisão

Migrar para o **Material UI 9.5** em uma branch, em passos que deixam o projeto verde a cada commit:

1. **MUI 7** (`@mui/material` e `@mui/icons-material`): renomear `Grid2` → `Grid`. As props antigas ainda
   funcionam na 7, então este passo isola a mudança de pacote e o Grid.
2. **MUI 9**: aplicar os codemods oficiais (`v9.0.0/system-props`, `deprecations/text-field-props`,
   `deprecations/list-item-text-props`), revisando o diff de cada um.
3. **Ajustes manuais**: ícones `*Outlined`, `nativeButton={false}` onde o botão vira link, conferir o menu lateral
   (largura dos ícones) e os testes que localizam selects pelo rótulo.
4. **Documentação**: este ADR passa a Aceito; README e `proximos-passos.md`.

**Conferência visual obrigatória**: capturas de todas as telas e diálogos principais no Chromium **antes** (MUI 6)
e **depois** (MUI 9), comparadas lado a lado, em desenvolvimento e no build de produção. Testes jsdom não pegam
mudança de layout.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Ficar no MUI 6 | Linha sem novidades; afasta o projeto das versões que o Dependabot e a documentação atual cobrem; a dívida cresce |
| Parar no MUI 7 | Metade do caminho: as props removidas na 9 já são as que a 7 marca como obsoletas; a 9 vem logo depois |
| Migrar tudo num commit só | Se algo quebrar, não se sabe se foi o pacote, o Grid ou um codemod; reverter fica caro |
| Trocar de biblioteca de componentes | Custo enorme sem ganho para o produto |

## Consequências

- **Positivas**: frontend inteiro em linhas atuais; props no formato `slots`/`slotProps` e `sx`, o padrão das
  versões novas; Dependabot volta a propor só correções do MUI.
- **Negativas / riscos**:
  - **Navegadores**: o MUI 9 deixa de suportar navegadores anteriores a Chrome 117, Edge 121, Firefox 121 e
    Safari 17 (2023). Computadores de chão de fábrica com navegador desatualizado podem ter problemas.
  - Mudanças de aparência sutis (largura de ícones do menu, espaçamentos); a conferência visual existe para isso.
  - Codemods mexem em muitos arquivos de uma vez (31 só com `fontWeight`); o diff é grande, mas mecânico.
- **Rollback**: reverter o PR; nenhuma mudança de API nem de banco.

## Execução (2026-10-31)

Feita em dois commits de código (MUI 7.3, depois 9.5) e um de teste. O que a execução revelou:

- **O CLI do `@mui/codemod` 9.5 não roda** (`yargs.command is not a function`). As transformações foram aplicadas
  chamando o `jscodeshift` diretamente com os arquivos do pacote. A saída do recast vem com aspas duplas, ponto e
  vírgula e `sx` aberto em várias linhas; foi ajustada ao estilo do projeto só nas linhas que o codemod mexeu.
- **O codemod de props de sistema pula valores calculados**: 11 `color={...}` na Typography (desvio, saldo,
  vencimento, alocação, cartões do dashboard) continuaram compilando — `color` é atributo HTML válido — mas deixaram
  de pintar o texto. Só a comparação de capturas mostrou; foram para o `sx`, e o relatório de consumo ganhou um teste
  que confere a cor do desvio. `color="primary"` (nome de paleta) continua válido na Typography.
- **"Troque `*Outline` por `*Outlined`" não vale para todo ícone**: `AddCircleOutlined` é o círculo **cheio**. Os
  equivalentes de mesmo desenho (traçado SVG idêntico) são `AddCircleOutlineOutlined` e `DeleteOutlineOutlined`.
- **O `Switch` passou a ter `role="switch"`** (MUI 7); o teste que o procurava como `checkbox` foi ajustado.
- **Nada do resto do risco previsto apareceu**: nenhum aviso de `nativeButton` nos botões-link, nenhuma mudança no
  menu lateral (a largura dos ícones já era definida por `sx`), nenhum teste quebrado pelo rótulo dos selects.

Resultado: 28 capturas (19 telas, 8 diálogos e um select aberto) iguais às do MUI 7 — até 4 pixels de ruído — e,
contra o MUI 6, só a suavização do texto dos chips (até 0,24% dos pixels); nenhum erro ou aviso no console, em
desenvolvimento e no build de produção. 377 testes, lint, build e `npm audit` sem vulnerabilidades.
