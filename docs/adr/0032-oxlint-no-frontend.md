# ADR-0032: Oxlint como linter do frontend

- **Status**: Aceito
- **Data**: 2026-11-06

## Contexto

`npm run lint` só checava os tipos. Regras de hooks e código suspeito não tinham barreira automática, e havia comentários `eslint-disable` para uma ferramenta que nem estava instalada.

O projeto usa TypeScript 7.0. O `typescript-eslint` 8.72 exige TypeScript abaixo de 6.1, então não dá para ler TSX com ESLint sem voltar a versão da linguagem.

## Decisão

Usar Oxlint, com versão fixa, em `npm run lint` e no CI. A regra `react/set-state-in-effect` fica como aviso, com teto em `--max-warnings`, que deve diminuir à medida que esses efeitos forem refeitos.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| ESLint + `typescript-eslint` | Exige TypeScript abaixo de 6.1 |
| ESLint com uma segunda versão do TypeScript só para o lint | Duas versões da linguagem no projeto, que podem discordar |
| Só `tsc` | Mantém o problema |

## Consequências

- Hooks e erros prováveis passam a barrar o PR.
- O Oxlint tem parser próprio e respeita os comentários `eslint-disable`, mas tem menos regras que dependem de informação de tipos, e as configurações do ecossistema ESLint não se aplicam diretamente.
- Quando o `typescript-eslint` aceitar o TypeScript 7, a troca pode ser reavaliada.
- O formatador segue fora: reformatar o código todo agora misturaria o histórico sem corrigir defeito.
