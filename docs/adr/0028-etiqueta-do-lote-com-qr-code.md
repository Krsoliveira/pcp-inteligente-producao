# ADR-0028: Etiqueta do lote com QR code

- **Status**: Aceito
- **Data**: 2026-10-31
- **Complementa**: ADR-0007 (lote) e ADR-0011 (rastreabilidade)

## Contexto

No chão de fábrica o lote precisa ser identificado fisicamente: quem pega uma caixa ou um tambor quer saber, sem
digitar nada, o que é, de onde veio, a validade e quanto ainda há. O sistema rastreia o lote de ponta a ponta, mas
não havia como levar essa informação do sistema até o material.

## Decisão

Cada lote ganha uma **etiqueta de 100 × 50 mm** com um **QR code que leva à tela do lote no sistema**, impressa pelo
botão "Imprimir etiqueta" na tela do lote.

- **O QR carrega só o link** (`<endereço do sistema>/lotes/<id>`), não os dados. Saldo e status mudam a cada
  consumo; o link sempre mostra o estado atual. E quem só fotografa a etiqueta não lê nada do sistema: a tela pede
  login e respeita as permissões (ADR-0020).
- **Os dados para leitura humana vão impressos ao lado**: número do lote, código e descrição do material,
  quantidade, fabricação, validade e a origem (nota fiscal e fornecedor, ou a ordem). A nota fiscal ou a ordem
  nunca é abreviada; só o nome do fornecedor pode ser.
- **Impressão pelo navegador** (`@page { size: 100mm 50mm }`): só a etiqueta sai, numa página do tamanho exato.
  Serve para impressora térmica de etiquetas e também para impressora comum.
- **Gerado no frontend** com `qrcode.react` 4.2 (ISC, sem dependências, sem vulnerabilidades conhecidas), em SVG, que
  sai nítido em qualquer resolução. Nenhuma mudança na API nem no banco.
- **Login volta para a página pedida**: quem escaneia sem estar logado passa pelo login e cai no lote, não no
  Dashboard. Só caminhos internos são aceitos como destino ("//site" e "/\site" são recusados), para que um link
  não use o login para levar o usuário a outro site.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Dados do lote dentro do QR | Ficam desatualizados (saldo, status) e expõem dados a qualquer leitor |
| Link e dados no mesmo QR | QR maior e mais denso, difícil de ler em etiqueta pequena, com as mesmas desvantagens |
| GS1-128 / GS1 DataMatrix | Padrão do varejo, exige GTIN, que o cadastro de material não tem |
| Gerar a imagem no backend (ZXing) | Endpoint e dependência novos no servidor para algo que o navegador faz sozinho |
| Folha A4 | Não é o formato usado para identificar embalagens no chão de fábrica |

## Consequências

- **Positivas**: lote identificado fisicamente; leitura com a câmera do celular, sem aplicativo; rastreabilidade a um
  toque do material.
- **Negativas / riscos**:
  - O QR aponta para o endereço em que a etiqueta foi impressa. Etiquetas impressas em `localhost` não funcionam em
    outro aparelho; em produção valerão enquanto o domínio não mudar.
  - O tamanho depende da impressora respeitar o tamanho de página; em impressora comum é preciso recortar.
- **Rollback**: remover o botão e o componente; nada muda na API nem no banco. Etiquetas já impressas continuam
  abrindo a tela do lote.
- **Pendências**: impressão de várias etiquetas de uma vez (todos os lotes de uma nota fiscal ou de uma ordem) e
  leitura do QR dentro do próprio sistema (ex.: escolher o lote no lançamento de consumo).
