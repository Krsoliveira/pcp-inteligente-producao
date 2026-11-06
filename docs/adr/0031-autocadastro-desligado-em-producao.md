# ADR-0031: Autocadastro desligado em produção

- **Status**: Aceito
- **Data**: 2026-11-06
- **Complementa**: ADR-0020 (controle de acesso) e ADR-0021 (troca de senha)

## Contexto

`POST /api/v1/auth/registrar` é público e dá o perfil Visualizador a quem se cadastrar (ADR-0020). Serve bem em desenvolvimento
e demonstração, mas num sistema interno de fábrica significa que qualquer pessoa com o endereço cria uma conta e lê ordens,
estoque e notas fiscais. Além disso, a resposta 409 de e-mail já cadastrado revela quem tem conta.

## Decisão

- Propriedade `pcp.seguranca.autocadastro-habilitado`: ligada por padrão e **desligada no perfil `prod`**. Quem quiser ligar em
  produção define `AUTOCADASTRO_HABILITADO=true`.
- Desligado, o caso de uso `RegistrarUsuario.autocadastrar` responde 403 antes de olhar o corpo, sempre com a mesma mensagem.
  O endpoint continua em `/api/v1`: o contrato só cresce.
- Conta nova em produção nasce pelas mãos de um gestor (`USUARIO_GERENCIAR`), com senha provisória (ADR-0021).
- O build de produção do frontend não oferece a tela (`VITE_AUTOCADASTRO=false` em `.env.production`). É só para não mostrar uma
  tela que não funcionaria: a barreira é o servidor.

## Alternativas consideradas

| Alternativa | Por que foi descartada |
|---|---|
| Remover o endpoint | Quebra o contrato público; exigiria `/v2` |
| Manter ligado em produção | Qualquer pessoa leria os dados; continuaria revelando quem tem conta |
| Autocadastro com aprovação de um gestor | Mais telas e estados para um problema que a criação por gestor já resolve |
| Endpoint público de configuração para o frontend | Mais um endpoint público para esconder um link |

## Consequências

- **Positivas**: sem conta anônima em produção; o cadastro deixa de revelar quem tem conta; dev e testes não mudam.
- **Negativas**: uma demonstração pública precisa definir `AUTOCADASTRO_HABILITADO=true` e gerar o build com `VITE_AUTOCADASTRO`
  diferente de `false`; as duas configurações precisam andar juntas.
