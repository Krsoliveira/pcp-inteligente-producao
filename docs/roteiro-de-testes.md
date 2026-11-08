# Roteiro de testes manuais

Para rodar antes de uma entrega e a cada rodada de testes de aceite. Marque cada item e anote o que achar na tabela do fim. Faça em **desktop** (1366 e 1920 px) e em **celular** (largura de 360 a 414 px, retrato).

Ambiente: https://pcp.krsoliveira.com.br (produção). Use dados de teste claramente identificados (prefixo `TESTE-`) e apague ou desative depois.

## Como anotar um achado

Tela, passo, o que esperava, o que aconteceu, aparelho e largura, hora aproximada (para achar no log) e, se der, uma captura. O `X-Request-ID` da resposta (aba Rede do navegador) liga o achado à linha de log.

---

## 1. Segurança dos dados

| # | Passo | Esperado | OK |
|---|---|---|---|
| 1.1 | Entrar com senha errada 3 vezes | Mensagem "E-mail ou senha incorretos."; nunca diz qual dos dois errou | ☐ |
| 1.2 | Errar a senha muitas vezes seguidas | Aparece o aviso de limite com o tempo de espera; depois volta a funcionar | ☐ |
| 1.3 | Desativar um usuário de teste e tentar entrar com ele | Mesma mensagem da senha errada | ☐ |
| 1.4 | Desativar um usuário que está logado em outra aba | Na próxima ação ele volta ao login | ☐ |
| 1.5 | Tentar desativar o último Administrador ativo | Recusado, com a explicação | ☐ |
| 1.6 | Entrar com um perfil só de consulta e abrir cada menu | Só aparece o que o perfil permite; botões de escrita não aparecem | ☐ |
| 1.7 | Com o perfil de consulta, colar na barra do navegador a URL de uma tela restrita (Acessos, Auditoria) | Tela de acesso negado ou volta ao início; nenhum dado aparece | ☐ |
| 1.8 | Perfil que não pode ver materiais abre uma lista técnica | Vê o código e a descrição dos materiais; não consegue abrir o cadastro do material | ☐ |
| 1.9 | Criar um perfil com mais permissões do que o seu | Recusado | ☐ |
| 1.10 | Abrir o mesmo perfil em duas abas, salvar na primeira e depois na segunda | A segunda avisa que a versão mudou e oferece carregar a atual | ☐ |
| 1.11 | Idem para tipo de ordem | O mesmo aviso | ☐ |
| 1.12 | Trocar a senha e usar a aba que ficou aberta com a sessão antiga | A sessão antiga é derrubada | ☐ |
| 1.13 | Colar `<script>alert(1)</script>` e `'; DROP TABLE x;--` em campos de texto | Salva como texto; nada executa; aparece igual ao digitado | ☐ |
| 1.14 | Sair e usar o botão Voltar do navegador | Não mostra dados da sessão encerrada | ☐ |
| 1.15 | Tentar `/swagger-ui.html` e `/v3/api-docs` em produção | Não existem | ☐ |

## 2. Fluxo completo, do cadastro à produção

Use sempre o mesmo conjunto de dados e confira os números em cada etapa.

| # | Passo | Conferir | OK |
|---|---|---|---|
| 2.1 | Cadastrar um material acabado e dois componentes | Aparecem na lista, com código e unidade corretos | ☐ |
| 2.2 | Código de material repetido | Recusado, mensagem clara | ☐ |
| 2.3 | Criar a lista técnica do acabado com os dois componentes e a quantidade base | Itens e quantidades salvos como digitados | ☐ |
| 2.4 | Criar uma segunda versão, comparar com a primeira e ativar | A comparação mostra as diferenças; só uma versão fica ativa | ☐ |
| 2.5 | Registrar a entrada de uma nota fiscal dos componentes | O estoque sobe exatamente a quantidade da nota | ☐ |
| 2.6 | Criar uma ordem de produção do acabado | A ordem nasce com o status inicial e a lista ativa | ☐ |
| 2.7 | Liberar e iniciar a ordem | Cada mudança aparece no histórico | ☐ |
| 2.8 | Registrar o consumo dos componentes | O estoque baixa; o lote consumido é o esperado | ☐ |
| 2.9 | Consumir mais do que há em estoque | Recusado, sem alterar nada | ☐ |
| 2.10 | Concluir a ordem | Gera o lote do acabado; a quantidade e a validade batem | ☐ |
| 2.11 | Conferir a genealogia do lote | Mostra de quais lotes de componente ele veio | ☐ |
| 2.12 | Cancelar uma ordem em andamento | Status e histórico corretos; estoque coerente | ☐ |
| 2.13 | Clicar em duas ações da mesma ordem quase juntas (duas abas) | Só uma vale; a outra recebe aviso claro | ☐ |
| 2.14 | Conferir o painel depois de tudo | Os totais batem com as listas; cada número abre a lista que o originou | ☐ |
| 2.15 | Conferir a Auditoria | Cada passo acima tem autor e horário | ☐ |
| 2.16 | Imprimir a etiqueta do lote e abrir o QR no celular | Leva ao lote certo; se não estiver logado, volta à página pedida | ☐ |

Valores de borda para repetir nos campos de quantidade: `0`, `-1`, `0,001`, `1.234.567,89`, `999999999999`, vazio, texto.

## 3. Desktop e celular

| # | Passo | Esperado | OK |
|---|---|---|---|
| 3.1 | Cada tela em 1366 e 1920 px | Sem rolagem horizontal da página; tabelas rolam dentro do contêiner | ☐ |
| 3.2 | Cada tela em 360 px | Nada cortado; menu acessível; botões com tamanho tocável | ☐ |
| 3.3 | Formulários no celular | O teclado correto aparece (numérico, e-mail); o campo focado não fica escondido pelo teclado | ☐ |
| 3.4 | Girar o celular | A tela se ajusta sem perder o que foi digitado | ☐ |
| 3.5 | Tabelas com muitas colunas no celular | Dá para ver o identificador e rolar para o resto | ☐ |
| 3.6 | Navegar só pelo teclado (Tab, Enter, Esc) no desktop | O foco sempre aparece; diálogos abrem e fecham pelo teclado | ☐ |
| 3.7 | Zoom de 200% no desktop | Continua utilizável | ☐ |
| 3.8 | Conexão lenta (3G simulado no navegador) | Há indicação de carregamento; nada fica em branco sem aviso | ☐ |
| 3.9 | Perder a conexão no meio de um envio | Mensagem de erro; o que foi digitado não se perde | ☐ |

## 4. Clareza para o usuário

Faça esta parte como se nunca tivesse visto o sistema. Se parar para pensar, anote.

| # | Pergunta | OK |
|---|---|---|
| 4.1 | Dá para entender o que cada menu faz só pelo nome? | ☐ |
| 4.2 | Telas vazias explicam o que fazer primeiro? | ☐ |
| 4.3 | Cada mensagem de erro diz o que aconteceu e o que fazer? | ☐ |
| 4.4 | Ações que apagam ou desativam dizem o que vai acontecer e quem é afetado? | ☐ |
| 4.5 | O mesmo conceito tem sempre o mesmo nome em todas as telas? | ☐ |
| 4.6 | Os status (ordem, lote, lista) são distinguíveis sem depender só da cor? | ☐ |
| 4.7 | Datas, números e unidades estão no formato brasileiro e sem ambiguidade? | ☐ |
| 4.8 | Dá para voltar e desfazer um engano sem ajuda? | ☐ |

## 5. Desempenho percebido

| # | Passo | Meta | Medida |
|---|---|---|---|
| 5.1 | Abrir o login depois de limpar o cache | até 3 s em rede comum | |
| 5.2 | Abrir o painel | até 2,5 s | |
| 5.3 | Abrir a lista de ordens e de lotes | até 2 s | |
| 5.4 | Filtrar e paginar listas grandes | resposta imediata, sem travar | |
| 5.5 | Registrar um consumo | confirmação em até 1 s | |
| 5.6 | Cadastro de 50 materiais seguidos | sem lentidão crescente | |

Depois da rodada, peça o log de `pcp.desempenho`: toda linha `requisicao_lenta` é uma rota que passou de 1 s.

## 6. Logs depois da rodada

No painel do Railway, filtre o backend por:

- `pcp.seguranca` para ver logins, falhas, acessos negados e limites excedidos;
- `pcp.desempenho` para ver requisições lentas;
- `ERROR` para erros inesperados (cada um com o `requestId`).

Confirme que nenhuma linha traz senha, token, e-mail inteiro ou dado pessoal.

---

## Achados

| # | Tela / passo | Esperado | Aconteceu | Aparelho e largura | Gravidade | requestId |
|---|---|---|---|---|---|---|
| | | | | | | |
