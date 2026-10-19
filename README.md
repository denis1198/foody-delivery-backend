# Foody Delivery — Backend

API REST de um marketplace de delivery: clientes fazem pedidos em restaurantes, e cada restaurante gerencia
cardápio, pedidos e acompanha uma dashboard financeira.

> Front-end (React): https://github.com/denis1198/foody-delivery-frontend

**Stack:** Java 21, Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation), jjwt, SQLite + Hibernate.

## Como rodar

Pré-requisito: **JDK 21+** (o Maven Wrapper dispensa instalar Maven).

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

A API sobe em http://localhost:8080. Com o banco vazio, o seed cria **6 restaurantes com cardápio, 41 clientes e
~90 dias de pedidos** (~9 mil), com picos no almoço/jantar e dias fracos no início da semana, para a dashboard ter
histórico desde o primeiro acesso.

| Conta de demonstração | Senha | Papel |
|---|---|---|
| `cliente@foody.com` | `demo123` | Cliente |
| `vendedor@foody.com` | `demo123` | Dono da Cantina da Nonna |
| `sushi@` `burger@` `mineiro@` `doces@` `verde@foody.com` | `demo123` | Donos dos demais restaurantes |

| Variável de ambiente | Padrão | Descrição |
|---|---|---|
| `JWT_SECRET` | segredo de desenvolvimento | Chave HMAC do JWT (mín. 32 caracteres). **Defina em produção.** |
| `JWT_EXPIRATION` | `PT24H` | Validade do token (ISO-8601) |
| `DB_PATH` | `./data/foody.db` | Caminho do arquivo SQLite (apague para recriar o seed) |
| `SEED_DEMO_DATA` | `true` | Popula o banco vazio com os dados de demonstração |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origens liberadas para o front-end |

### Testes

```bash
./mvnw test
```

27 testes: regras de status, fórmulas financeiras (`OrderPricingTest`), fluxo completo cliente ↔ vendedor com
permissões (`OrderFlowIntegrationTest`) e a dashboard sobre uma semana de pedidos com datas controladas
(`DashboardIntegrationTest`).

## Papéis e autenticação

Cadastro com `role`: `CUSTOMER` (padrão) ou `SELLER`. O vendedor informa também `restaurantName` e
`restaurantCategory`, e o restaurante é criado junto. O login devolve `{ token, user }` com o papel, que o
front-end usa para escolher a área. Rotas `/api/seller/**` exigem `SELLER`; `/api/orders/**` exigem `CUSTOMER`
(403 caso contrário). Pedido de outro cliente ou de outro restaurante responde 404.

## Endpoints

Todas as rotas exigem `Authorization: Bearer <token>`, exceto cadastro e login. Erros seguem o formato
[Problem Details (RFC 9457)](https://www.rfc-editor.org/rfc/rfc9457), com `errors` por campo na validação.

| Quem | Método | Rota | Descrição |
|---|---|---|---|
| todos | `POST` | `/api/auth/register` · `/api/auth/login` | Cadastro e login |
| todos | `GET` | `/api/auth/me` | Usuário autenticado |
| todos | `GET` | `/api/restaurants?category=&q=` | Vitrine (abertos primeiro) |
| todos | `GET` | `/api/restaurants/{id}` | Restaurante + cardápio público (só itens disponíveis, sem custos) |
| cliente | `POST` | `/api/orders` | Cria pedido: `{ restaurantId, items: [{ productId, quantity, notes }], deliveryAddress, paymentMethod, notes }` |
| cliente | `GET` | `/api/orders/mine` · `/api/orders/{id}` | Pedidos do cliente |
| cliente | `PATCH` | `/api/orders/{id}/cancel` | Cancela enquanto `RECEBIDO` |
| vendedor | `GET` `PUT` | `/api/seller/restaurant` | Perfil, entrega e percentuais |
| vendedor | `PATCH` | `/api/seller/restaurant/open` | Abre/fecha a loja |
| vendedor | `GET` | `/api/seller/menu` | Cardápio completo, com custos e percentuais |
| vendedor | `POST` `PUT` `DELETE` | `/api/seller/menu/categories[/{id}]` | Categorias |
| vendedor | `POST` `PUT` `DELETE` | `/api/seller/menu/products[/{id}]` | Produtos (excluir arquiva, preservando o histórico) |
| vendedor | `PATCH` | `/api/seller/menu/products/{id}/availability` | Disponível/esgotado |
| vendedor | `GET` | `/api/seller/orders` | Quadro: pedidos em andamento + finalizados hoje |
| vendedor | `GET` | `/api/seller/orders?from=YYYY-MM-DD&to=YYYY-MM-DD` | Histórico: todos os pedidos da loja criados no período (até 366 dias), em qualquer status |
| vendedor | `GET` · `PATCH` | `/api/seller/orders/{id}` · `/{id}/status` | Detalhe (com financeiro) e avanço de status |
| vendedor | `GET` | `/api/seller/dashboard?from=YYYY-MM-DD&to=YYYY-MM-DD` | Métricas do período |

Regras do pedido: preço e custo vêm sempre do cardápio (o que o cliente envia é ignorado); restaurante fechado,
produto indisponível ou de outro restaurante respondem **409**.

### Fluxo de status

```
RECEBIDO ──► EM_PREPARO ──► SAIU_PARA_ENTREGA ──► ENTREGUE
    │             │                 │
    └─────────────┴─────────────────┴──► CANCELADO
```

Uma etapa por vez, sem voltar; `ENTREGUE` e `CANCELADO` são finais (transição inválida: **409**). O cliente só
cancela antes de o restaurante aceitar. As respostas trazem `nextStatuses` com as ações permitidas para quem pede.

## Métricas

`OrderPricing` calcula e o pedido guarda, no momento da compra:

| Campo | Fórmula |
|---|---|
| Faturamento (`subtotal`) | Σ preço × quantidade (sem taxa de entrega) |
| Comissão | faturamento × `commissionPercent` (padrão 12%) |
| Taxa de pagamento | faturamento × `paymentFeePercent` (3,2%), só Pix e cartão |
| Líquido | faturamento − comissão − taxa de pagamento |
| CMV | Σ custo × quantidade |
| Imposto | faturamento × `taxPercent` (6%) |
| Lucro bruto | líquido − CMV − imposto |

`GET /api/seller/dashboard` agrega esses valores no fuso de Brasília e devolve:

- `summary` e `previous` (mesmo intervalo imediatamente anterior): faturamento, líquido, lucro, margem, vendas,
  itens, ticket médio, cancelamentos, comissão, taxas, impostos, CMV, entregas e ROI;
- `revenueByDay` (dias sem venda com zero), `ordersByHour` (0–23h), `weekdays` (média por dia da semana),
  `weakestDays` (3 datas com menos vendas), `topProducts` (15, com lucro proporcional às taxas do pedido) e
  `statusBreakdown`.

O dia corrente, ainda incompleto, fica fora das comparações entre dias.

## Estrutura

```
src/main/java/com/foody/delivery/
├── auth/        # cadastro (cliente ou vendedor), login, /me
├── security/    # JWT, filtro, regras por papel, CORS
├── user/        # usuário e papéis
├── restaurant/  # restaurante, vitrine e configurações do vendedor
├── menu/        # categorias e produtos
├── order/       # pedidos, status, OrderPricing
├── dashboard/   # agregações da dashboard
├── seed/        # dados de demonstração
└── common/      # exceções, ProblemDetail, fuso de negócio
```

> Observação sobre SQLite: o dialeto community do Hibernate gera chaves primárias sem tipo no caminho de
> `create`; só `INTEGER PRIMARY KEY` vira autoincremento no SQLite. Por isso os ids declaram `integer` e o
> esquema usa `ddl-auto: update` (inclusive nos testes, com um arquivo novo por execução).
