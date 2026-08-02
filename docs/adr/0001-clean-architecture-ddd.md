# ADR-0001: Clean Architecture + DDD no backend

- **Status**: Aceito
- **Data**: 2026-08-02

## Contexto

O backend concentrará regras de negócio de PCP (Planejamento e Controle da Produção)
que precisam ser testáveis, compreensíveis por quem conhece o negócio e independentes
de detalhes técnicos (banco, filas, provedor de IA). O projeto também é peça de
portfólio: a organização do código deve demonstrar domínio de arquitetura, não apenas
de framework.

## Decisão

Adotar **Clean Architecture** com três camadas (`domain/`, `application/`,
`infrastructure/`) e modelagem orientada a **DDD (Domain-Driven Design)**, usando o
vocabulário do PCP nas entidades (`OrdemProducao`, `CentroDeTrabalho`, etc.).
Dependências apontam sempre para o domínio; o domínio declara interfaces (portas) que
a infraestrutura implementa.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Arquitetura em camadas clássica (controller → service → repository, tudo acoplado ao Spring/JPA) | Mais rápida no início, mas as regras de negócio ficam presas ao framework e difíceis de testar isoladamente |
| Arquitetura hexagonal "pura" com módulos Maven separados por camada | Mesmos benefícios, porém com burocracia de build desproporcional ao tamanho do projeto (YAGNI) |

## Consequências

- **Positivas**: regras de domínio testáveis sem subir Spring ou banco; troca de
  detalhes técnicos (ex.: provedor de IA) sem tocar o domínio; código legível para
  quem conhece PCP.
- **Negativas / riscos**: mais classes e mapeamentos (entidade de domínio ↔ entidade
  JPA ↔ DTO); exige disciplina para não "vazar" anotações de framework para o domínio.
