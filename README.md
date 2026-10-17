# Foody Delivery — Backend

API REST de gestão de pedidos de delivery, com cadastro e login de usuários (JWT) e persistência em SQLite.

> Front-end (React): https://github.com/denis1198/foody-delivery-frontend

**Stack:** Java 21, Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation), jjwt, SQLite + Hibernate.

## Como rodar

Pré-requisito: **JDK 21+**. Não é preciso instalar o Maven, porque o projeto usa o Maven Wrapper.

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

A API sobe em http://localhost:8080. O banco SQLite é criado automaticamente em `data/foody.db`.

| Variável de ambiente | Padrão | Descrição |
|---|---|---|
| `JWT_SECRET` | segredo de desenvolvimento | Chave HMAC do JWT (mín. 32 caracteres). **Defina em produção.** |
| `JWT_EXPIRATION` | `PT24H` | Validade do token (ISO-8601) |
| `DB_PATH` | `./data/foody.db` | Caminho do arquivo SQLite |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origens liberadas para o front-end |

### Testes

```bash
./mvnw test
```

Testes unitários das regras de status e testes de integração (MockMvc) que cobrem cadastro, login, CRUD de
pedidos e as respostas de erro 400, 401, 404 e 409.

## Endpoints

Todas as rotas, exceto cadastro e login, exigem `Authorization: Bearer <token>`.
Os erros seguem o formato [Problem Details (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457). Erros de
validação trazem também um mapa `errors` com a mensagem de cada campo.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/auth/register` | Cadastra usuário (`name`, `email`, `password`) e retorna `{ token, user }` |
| `POST` | `/api/auth/login` | Login com `email` e `password` e retorna `{ token, user }` |
| `GET` | `/api/auth/me` | Dados do usuário autenticado |
| `POST` | `/api/orders` | Cria pedido |
| `GET` | `/api/orders` | Lista todos os pedidos, mais recentes primeiro. Filtro opcional: `?status=EM_PREPARO` |
| `GET` | `/api/orders/{id}` | Busca pedido por ID |
| `PATCH` | `/api/orders/{id}/status` | Atualiza o status: `{ "status": "EM_PREPARO" }` |

Exemplo de criação:

```json
POST /api/orders
{
  "customerName": "João Silva",
  "deliveryAddress": {
    "street": "Rua das Flores", "number": "123", "complement": "Apto 4",
    "neighborhood": "Centro", "city": "São Paulo", "zipCode": "01000-000"
  },
  "items": [
    { "productName": "Pizza Margherita", "quantity": 2, "unitPrice": 45.90 },
    { "productName": "Refrigerante", "quantity": 1, "unitPrice": 8.50 }
  ]
}
```

A resposta traz o pedido completo, com `total` (calculado no servidor), `status` e `nextStatuses`, que são
as transições permitidas a partir do status atual.

### Fluxo de status

```
RECEBIDO ──► EM_PREPARO ──► SAIU_PARA_ENTREGA ──► ENTREGUE
    │             │                 │
    └─────────────┴─────────────────┴──► CANCELADO
```

O pedido nasce como `RECEBIDO` e avança uma etapa por vez, sem pular nem voltar. Pode ser `CANCELADO`
antes da entrega. `ENTREGUE` e `CANCELADO` são finais. Transições inválidas retornam **409 Conflict**.

## Decisões de projeto

- **JWT stateless**: o token vai no header `Authorization`, o que dispensa sessão no servidor e proteção
  CSRF. As senhas são guardadas com BCrypt.
- **Visão de operador**: qualquer usuário autenticado vê e gerencia todos os pedidos. Quem registrou cada
  pedido fica salvo em `createdBy`.
- **Regras no domínio**: a máquina de estados fica no enum `OrderStatus` e é validada pela entidade `Order`.
- **Esquema gerado pelo Hibernate** (`ddl-auto: update`), o que basta para o escopo do projeto. Numa
  evolução, o próximo passo seria versionar o esquema com Flyway.

## Estrutura

```
src/main/java/com/foody/delivery/
├── auth/       # cadastro, login, /me
├── security/   # JWT, filtro de autenticação, CORS, SecurityConfig
├── user/       # entidade e repositório de usuários
├── order/      # entidades, regras de status, serviço e controller de pedidos
└── common/     # exceções e handler global de erros
```
