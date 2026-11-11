# ADR-0037: Unidade de medida padronizada em maiúsculas

- **Status**: Aceito
- **Data**: 2026-11-10

## Contexto

A unidade de medida é texto livre em quatro tabelas (`material`, `item_lista_tecnica`, `consumo_material` e `lote`),
só com `trim()`. "kg", "KG" e "Kg" viram unidades diferentes: o estoque, a lista técnica e o consumo ficam
inconsistentes, e a comparação de unidades entre componentes (necessária para o aviso da soma da lista) fica frágil.
Quem cadastra também não sabe quais unidades já existem no sistema.

## Decisão

- A unidade é normalizada em **maiúsculas e sem espaços nas pontas** no domínio (`UnidadeDeMedida.normalizar`), em
  todo ponto de entrada: material, item da lista técnica, consumo e lote. Aceita letras, números, `%`, `²`, `³`, `/`,
  `.` e `-`, até 10 caracteres (o tamanho da coluna). Fora disso, regra de negócio recusa (422).
- A **migração V23** normaliza os dados existentes (`UPPER(TRIM())`) nas quatro tabelas.
- Novo `GET /api/v1/materiais/unidades`: as unidades já cadastradas, sem duplicidade e em ordem alfabética
  (material e itens de lista), com a permissão de visualizar material. O contrato só cresce.
- As telas de material e de item da lista oferecem essas unidades como sugestão (o usuário ainda pode digitar uma
  nova) e já mostram o que digitou em maiúsculas.
- Ao criar a lista técnica, quando o material e todos os componentes são de peso (KG, G, MG, T) ou todos de volume
  (L, ML), a tela **avisa** se a soma dos componentes difere da quantidade base. Só avisa: não bloqueia e não grava
  nada.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Tabela de unidades (catálogo com conversão) | Modelo novo, telas de cadastro e migração maior, sem necessidade hoje; a conversão só serve ao aviso, que já cabe numa tabela fixa na tela |
| Manter minúsculas (kg) | O dono pediu maiúsculas e é o padrão que aparece nas etiquetas e documentos |
| Só normalizar na tela | O servidor é a barreira: a API chamada direto continuaria aceitando "kg" e "KG" |
| Bloquear a lista quando a soma não bate | Decisão do dono: só avisar; há processos com perda ou componentes em unidades diferentes |

## Consequências

- **Positivas**: uma unidade por significado em todo o sistema; sugestão evita duplicidade nova; base para comparar
  unidades.
- **Negativas / riscos**:
  - A V23 apaga a caixa original dos dados existentes e não se desfaz por SQL. A caixa original não carrega
    informação, e o backup antes do release cobre o caso.
  - Eventos de auditoria antigos guardam o texto antigo (a trilha é imutável).
  - O valor da unidade devolvido pela API muda de caixa (o campo é o mesmo); só o frontend do projeto consome.
  - Unidades já cadastradas com caracteres fora do padrão (ex.: espaço no meio) passam a ser recusadas ao editar.
- **Rollback**: antes do release, descartar a branch. Depois, a imagem anterior lê normalmente (maiúsculas são texto
  válido); os dados só voltam à caixa original pelo backup.
