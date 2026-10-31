# ADR-0017: Testes automáticos do frontend com Vitest e Testing Library

- **Status**: Aceito
- **Data**: 2026-10-21

## Contexto

O frontend só tinha `build` (checagem de tipos) como verificação. As mudanças de
interface do dia 21/10 (mensagens para leigos, contagem regressiva no login, botão Voltar,
filtros na URL, paginação) dependiam de olhar a tela manualmente. Além disso, o script
`npm run lint` (`tsc --noEmit` na raiz) não verificava nada: o `tsconfig.json` raiz só
tem referências, então a checagem real de tipos era a do `npm run build`.

## Decisão

- **Vitest 4.1.12 + jsdom 27.4.0 + Testing Library** (`react` 16.3.3, `user-event` 14.6.8,
  `jest-dom` 6.6.3), versões exatas, só em `devDependencies`.
- Vitest porque já usamos Vite (mesma configuração, sem transpilador à parte) e porque a
  API é a do Jest. Os testes importam `describe/it/expect` explicitamente (sem globais).
- **Testa-se comportamento, não detalhes de implementação:** o que a pessoa vê e faz
  (rótulos, botões, mensagens, URL), com as chamadas HTTP simuladas (`vi.mock` nos módulos
  de `api/`) e erros construídos como o Axios os entrega (`src/test/erros.ts`).
- Cobertura inicial: utilitários (mensagens de erro, formatação, URL), hooks (contagem
  regressiva, paginação local, filtros na URL), componentes (paginação, painel de consulta,
  subfiltro, cabeçalho com Voltar) e três telas (login, resultados de ordens, detalhe da
  lista técnica).
- `npm test` (`vitest run`) roda no CI, no job do frontend, depois do build.
- O script `lint` passou a `tsc -p tsconfig.app.json --noEmit --incremental false`, que
  checa os tipos de verdade (inclusive dos arquivos de teste).
- Os testes foram validados por **mutação manual**: quebrar de propósito a contagem
  regressiva, o tratamento do 5xx e a leitura da página na URL faz testes falharem.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Jest | Exige transformar TS/JSX e ESM à parte; Vitest reaproveita o Vite |
| Playwright/Cypress (E2E) | Valem a pena depois, mas pedem backend e banco no ambiente de teste e são lentos; os testes de componente cobrem a lógica de tela agora |
| Vitest 5, jsdom 30 e jest-dom 7 | Exigem Node 22+; o CI usa Node 20 (e esta máquina, 24.13, fica abaixo do mínimo do jsdom 30) |
| Vitest 3.x | Dentro da faixa do advisory GHSA-82fw-gwwq-j7x9 (corrigido na 4.1.11); a 4.1.12 aceita Node 20 |
| Deixar o `lint` como estava | Dava falsa sensação de checagem de tipos |

## Consequências

- **Positivas**: regressões da interface passam a quebrar o CI; a lógica de tela (erros,
  contagem regressiva, filtros na URL) tem rede de segurança; o `lint` volta a valer.
- **Negativas / riscos**:
  - Jsdom não é um navegador: layout, CSS e aparência (ex.: o gráfico do dashboard, que é
    desenhado em canvas) **não** são testados. Continua necessária revisão visual, ou E2E.
  - Subir o Node do CI para 22+ (o Node 20 já saiu de suporte) permitirá atualizar Vitest,
    jsdom e jest-dom; reavaliar junto.
  - Testes com temporizadores simulados exigem cuidado (um `act` por segundo); os da tela
    de login documentam o padrão.

## Atualização — 2026-10-31

O CI passou a rodar o frontend em Node 22 (#83), o que destravou as versões descartadas acima: Vitest 4.1 → 5.0,
jsdom 27 → 30 e jest-dom 6 → 7, junto com Vite 6 → 8 (e `@vitejs/plugin-react` 4 → 6), que o Vitest usa. Nenhum teste
precisou mudar. O jsdom 30 exige Node 22.22+ ou 24.15+, também na máquina de desenvolvimento. O Vite 8 empacota com
Rolldown; a divisão do build em pedaços passou de `manualChunks` para `rolldownOptions.output.codeSplitting`.
