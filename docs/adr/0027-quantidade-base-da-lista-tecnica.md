# ADR-0027: Quantidade base da lista técnica

- **Status**: Aceito
- **Data**: 2026-10-23
- **Complementa**: ADR-0007 (lista técnica) e ADR-0019 (versões de lista técnica)

## Contexto

A lista técnica guardava a quantidade de cada componente "por 1 unidade" do material produzido. Isso não serve a
quem produz em lote ou por peso: "para produzir 100 kg de tinta usa-se 80 kg de pigmento e 20 kg de solvente" não
cabe em "por 1 kg" sem números fracionários difíceis de conferir. Não havia onde informar para quanto o material
produzido valem os componentes.

## Decisão

A lista técnica ganha a **quantidade base**: para quantas unidades (ou kg, na unidade do material produzido) valem as
quantidades dos componentes.

- **Onde fica:** na **versão da lista** (cada versão pode ter a sua), pedida ao cadastrar a lista, e não no cadastro do
  material: a base é parte da receita, e a receita muda por versão. A nova versão copiada herda a base da versão de
  origem e ela pode ser ajustada.
- **Padrão 1:** `lista_tecnica.quantidade_base` (V17) é `NUMERIC(12,4) NOT NULL DEFAULT 1` com `CHECK > 0`. As listas
  existentes continuam significando "por 1 unidade"; nada muda nos consumos já calculados nem nas ordens existentes.
- **Consumo planejado:** `quantidade do item ÷ base × quantidade da ordem`, arredondado a 4 casas (meio para cima),
  as mesmas do banco. Com a base 1 é a conta de sempre. Se um componente arredondar para zero numa ordem pequena, a
  ordem é recusada (422) com a orientação de aumentar a quantidade, e nada é criado.
- **Validação:** maior que zero, até 8 dígitos inteiros e 4 casas decimais (domínio e API).
- **Comparação de versões (ADR-0019):** é **por unidade produzida**. 2 kg para 1 un é igual a 200 kg para 100 un; a
  conta é um produto cruzado, sem divisão. As quantidades mostradas são as de cada versão, e a tela avisa quando as
  bases diferem.
- **API (só cresce):** `POST /listas-tecnicas` aceita `quantidadeBase` opcional; `ListaTecnicaResponse` e o resumo de
  versão da comparação devolvem `quantidadeBase`.
- **Interface:** campo "Quantidade base" no cadastro da lista (com a unidade do material produzido na ajuda), a base
  no detalhe da lista e o aviso na comparação.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Lote mínimo de produção no material | Resolve outro problema (menor ordem permitida); o responsável pediu a base da receita |
| Base no cadastro do material | A receita muda por versão; a base precisa acompanhá-la |
| Obrigar a base 1 e usar frações | Frações como 0,8 kg por kg escondem a receita real e geram erro de digitação |
| Guardar a quantidade por unidade já convertida | Perderia a receita como o usuário a escreveu |

## Consequências

- **Positivas**: receitas em lote ou por peso escritas como o chão de fábrica as conhece; consumo planejado correto
  para qualquer tamanho de ordem.
- **Negativas / riscos**:
  - O arredondamento a 4 casas pode gerar diferenças mínimas em bases que não dividem exato (ex.: base 3).
  - Ordens muito pequenas com bases muito grandes são recusadas (mensagem clara).
- **Rollback**: reverter o código é seguro (a coluna tem padrão 1 e o código antigo a ignora). Desfazer o schema é uma
  migração nova que remove a coluna; listas com base diferente de 1 perderiam a informação de base, então a volta só
  é segura se nenhuma tiver sido cadastrada.
- **Pendências**: a tela de ordem poderia mostrar a prévia do consumo planejado ao escolher a versão.
