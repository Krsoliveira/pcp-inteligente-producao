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
| `ordens_producao.csv` | Ordens com status, datas e quantidade produzida |
| `consumos_material.csv` | Consumo planejado × real, com justificativa dos desvios |
| `lotes.csv` | Lotes das ordens concluídas (o número é gerado na carga) |

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
