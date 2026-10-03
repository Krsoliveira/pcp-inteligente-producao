# Dados

Dataset **sintético** do projeto — decisão registrada no
[ADR-0009](../docs/adr/0009-dataset-sintetico-modelo-expandido.md).

## Gerador

`scripts/gerar_dataset_sintetico.py` (Python 3.10+, somente biblioteca padrão) gera, de
forma determinística, os CSVs lidos pelo `DataLoader` do backend:

| Arquivo (`backend/src/main/resources/dados/`) | Conteúdo |
|---|---|
| `dataset.properties` | `data_referencia` ("hoje" do dataset) e semente |
| `materiais.csv` | Matérias-primas, semiacabados e produtos acabados |
| `tipos_ordem.csv` | Categorias de ordem (Produção Normal, Retrabalho, ...) |
| `listas_tecnicas.csv` / `itens_lista_tecnica.csv` | BOMs versionadas e seus componentes |
| `ordens_producao.csv` | Ordens com status, datas, quantidade produzida e data de conclusão |
| `consumos_material.csv` | Consumo planejado × real, com justificativa dos desvios e data do registro |
| `lotes.csv` | Lotes das ordens concluídas, com saldo (o número é gerado na carga) |
| `lotes_compra.csv` | Entradas de matéria-prima: fornecedor, NF, emissão, recebimento e saldo |
| `alocacoes_lote.csv` | Genealogia: quanto de cada lote saiu em cada consumo |

### Genealogia (ADR-0011)

O estoque é simulado em ordem cronológica: cada consumo é alocado aos lotes do
componente pela regra **FEFO** (vence primeiro, sai primeiro), usando só lotes
disponíveis antes do dia do consumo e dentro da validade. Faltando matéria-prima, o
gerador cria uma **compra** (lote com NF de um fornecedor do material); faltando
semiacabado, uma **ordem de reposição** concluída pouco antes, com seus próprios consumos
alocados. Ao final, garante saldo para os consumos pendentes da carteira em produção (o
cenário "atual" precisa permitir registrar consumo pela tela) e define o status de cada
lote pelo saldo — zerado é `CONSUMIDO`. A validação confere que todo consumo foi coberto
por inteiro e que saldo + alocações = quantidade em todos os lotes.

```bash
# Regerar os CSVs após alterar o gerador
python3 data/scripts/gerar_dataset_sintetico.py

# Verificar se os CSVs versionados estão em dia (usado no CI)
python3 data/scripts/gerar_dataset_sintetico.py --verificar
```

## Carga no banco

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev,seed
```

A carga é transacional e idempotente (não roda se já houver dados) e desloca todas as
datas para que `data_referencia` coincida com o dia da carga.
