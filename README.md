# LoanPro E-Commerce — Code Challenge

Enterprise-grade e-commerce application built with Java 17, Spring Boot 3.5, React, and PostgreSQL. Fully containerized with Docker Compose — clone, run one command, and the entire stack is up.

**CSV file downloaded on:** June 12, 2026

## Quick Start

**Prerequisites:** Docker and Docker Compose installed and running.

```bash
git clone https://github.com/ozava/loanpro-ecommerce.git
cd loanpro-ecommerce

# Copy the example environment file
cp .env.example .env
# If using Windows PowerShell: Copy-Item .env.example .env

# Build and start everything
docker compose up -d --build
```

The first build takes a few minutes (Maven dependencies + npm install). After that:

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | React UI (product admin + store) |
| Backend API | http://localhost:8080/api | REST endpoints |
| Swagger UI | http://localhost:8080/swagger-ui.html | Interactive API docs |
| Health Check | http://localhost:8080/actuator/health | App + DB status |

The database is seeded automatically with 96 products from the provided CSV file via Flyway migrations.

To stop everything: `docker compose down` (add `-v` to also reset the database).

## Running Tests

```bash
# Unit tests (no database needed, runs anywhere)
cd backend
./mvnw test

# Karate API integration tests (requires the app running)
# Terminal 1: docker compose up -d --build
# Terminal 2:
cd backend
./mvnw test -Dtest=KarateTestRunner
```

**Unit tests (31 tests):** JUnit 5 + Mockito + AssertJ covering `OrderService`, `ProductService`, `PaymentPortResolver`, and `CsvImportService`. Pure unit tests with mocked dependencies — no Spring context, no database.

**Karate integration tests:** HTTP-level tests that exercise the full stack (request → controller → service → database → response). Cover both `ProductController` and `OrderController` including edge cases like HTML injection rejection, duplicate SKU conflicts, insufficient stock, and case-insensitive payment method resolution.

## Architecture

### Hexagonal Architecture

The codebase follows a strict Hexagonal (Ports & Adapters) architecture. Business logic never depends on infrastructure — it's the other way around.

```
com.loanpro.ecommerce/
├── domain/                          ← Core: entities + repository interfaces
│   ├── entity/                        Product, Category, Order, OrderItem
│   └── repository/                    JPA repository interfaces
├── application/                     ← Use cases: services + DTOs + ports
│   ├── dto/                           Request/Response records
│   ├── port/out/                      PaymentPort (output port interface)
│   ├── service/                       OrderService, ProductService, CsvImportService,
│   │                                  PaymentPortResolver
│   └── exception/                     Business exceptions
└── infrastructure/                  ← Adapters: web + payment providers
    ├── web/                           REST controllers, GlobalExceptionHandler
    └── adapter/payment/               FakeStripeAdapter, FakePaypalAdapter,
                                       FakeMercadoPagoAdapter
```

**Why Hexagonal?** It enforces a clear dependency rule. Adding a new payment provider means creating one adapter file — zero changes to `OrderService` or any controller. Swapping from fake payments to a real Stripe SDK is a single file replacement. The business logic is testable in isolation, which is why the unit tests use plain Mockito with no Spring context.

### Multi-Provider Payment System (Strategy Pattern)

The purchase flow supports multiple payment providers through a Strategy pattern resolved at runtime:

```
OrderRequest { paymentMethod: "stripe" }
    → PaymentPortResolver (Map<String, PaymentPort>)
        → FakeStripeAdapter implements PaymentPort
            → @CircuitBreaker(name = "stripe")
```

Each adapter is an independent Spring `@Service` implementing `PaymentPort`. The `PaymentPortResolver` builds a lookup map from all injected `PaymentPort` beans at startup using `@PostConstruct` — no `switch/case`, no conditional logic. Spring auto-discovers new adapters.

Each provider has its own Resilience4j circuit breaker with independent configuration, so a failure in one provider does not affect the others.

**Payment decline vs. infrastructure failure:** A declined payment (simulated at 10%) returns a `PaymentResult(false, ...)` and creates the order with status `"failed"` — this is a valid business response and does NOT open the circuit breaker. An infrastructure failure (timeout, connection refused) triggers the circuit breaker fallback, returns HTTP 503, and no order is created. This distinction is intentional — you don't want to cut off a payment provider just because customers have insufficient funds.

### Database Strategy

**PostgreSQL 16** with Flyway managing all schema changes. Spring Boot's Flyway is disabled (`flyway.enabled: false`) — migrations run in a dedicated Flyway Docker container to prevent dual execution conflicts.

Hibernate is set to `ddl-auto: validate` — it never modifies the schema, only validates that entity mappings match the existing tables. This forces all schema changes through version-controlled migrations.

**Indexes:** GIN index on `products(name || description)` for full-text search performance. B-tree indexes on `sku` (unique lookups) and `category_id` (joins).

### CSV Import Engine

The import pipeline handles dirty real-world data:

- Strips currency symbols (`$19.99` → `19.99`), trims whitespace, normalizes category names
- Validates every field independently — multiple errors per row are accumulated and reported together
- Batch inserts of 50 products per `REQUIRES_NEW` transaction with `EntityManager.clear()` to prevent memory issues on large files
- Pre-loads all existing SKUs and categories before the loop to avoid N+1 queries
- Returns an `errorFileId` (UUID) — the frontend downloads the error CSV with per-row failure details
- Error files expire after 10 minutes (scheduled cleanup)
- Performance tested: 1,005 rows processed in 2.84 seconds

### Input Validation & Security

- **SQL Injection:** Protected by design. All queries use JPA parameterized statements (`PreparedStatement`). Manually verified during development — malicious SQL input is stored as literal text, never executed.
- **XSS Prevention:** Two layers. Backend rejects HTML tags (`<`, `>`) on all string fields via `@Pattern` validation. Frontend uses React's built-in output escaping as a secondary defense.

### Pagination

All list endpoints return paginated responses with configurable `page`, `size`, and `sortBy` parameters. Default page size is 12 for the store catalog and 10 for the admin table. Essential for the 900+ product catalog.

## Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| Backend | Java 17, Spring Boot 3.5.15, Maven | Industry standard, mature ecosystem |
| Database | PostgreSQL 16 | ACID compliance, GIN indexes for search |
| Migrations | Flyway (Docker container) | Version-controlled schema, reproducible environments |
| ORM | Spring Data JPA (Hibernate 6.6) | CRUD productivity + `validate` mode for safety |
| Resilience | Resilience4j | Circuit breaker per payment provider |
| Frontend | React + Vite + Material UI | Fast dev experience, production-ready component library |
| Containerization | Docker Compose (multi-stage builds) | Backend ~150MB (JRE Alpine), Frontend ~30MB (nginx Alpine) |
| API Docs | SpringDoc OpenAPI 3.0 | Auto-generated from controllers, zero maintenance |
| Health | Spring Actuator | Standard health checks including DB connectivity |
| Testing | JUnit 5 + Mockito + AssertJ + Karate | Unit + API integration coverage |

## API Endpoints

### Products
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/products?page=0&size=12` | List products (paginated) |
| `GET` | `/api/products/{id}` | Get product by ID |
| `POST` | `/api/products` | Create product |
| `PUT` | `/api/products/{id}` | Update product |
| `DELETE` | `/api/products/{id}` | Delete product |
| `GET` | `/api/products/search?q=&page=0&size=12` | Search by name/description |
| `POST` | `/api/products/import` | Import CSV file |
| `GET` | `/api/products/import/errors/{id}` | Download error report |

### Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/orders` | Create order with payment |
| `GET` | `/api/orders/{id}` | Get order by ID |

### Operational
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Health check |
| `GET` | `/swagger-ui.html` | Interactive API docs |

## Key Decisions & Alternatives Considered

### Why `@Transactional` over Saga Pattern
This is a single-service, single-database application. Saga pattern solves distributed transactions across multiple services and databases — using it here would be over-engineering. If the system evolved to microservices (separate payment service, separate inventory service), Saga with orchestration would be the right migration path.

### Why Flyway in Docker, not in Spring Boot
Running Flyway as a separate Docker container prevents dual execution conflicts. The backend starts only after Flyway completes (`depends_on: condition: service_completed_successfully`). This cleanly separates concerns — the application validates the schema, it never creates or modifies it.

### Why manual CSV parser over Apache Commons CSV
The challenge constraints favor demonstrating understanding over using libraries. The custom parser handles quoted fields, mixed line endings, and dirty data — all within standard Java. For production, Apache Commons CSV would reduce maintenance burden.

### Why Hexagonal over traditional Layered Architecture
Layered architecture creates tight coupling between layers. Hexagonal enforces that business logic (domain + application) never imports infrastructure code. This is why adding three payment adapters required zero changes to `OrderService`. The architecture proves its value through the Strategy pattern integration.

## What I Would Add for Production

### Security Layer
Not implemented because the challenge does not require it, and adding JWT/OAuth increases complexity and development time significantly. For production, the approach would be:
- Spring Security with JWT for stateless authentication
- Role-based access: `ADMIN` for CRUD operations, `USER` for browsing and purchasing
- CORS restricted to known origins
- Rate limiting on sensitive endpoints (order creation, CSV import)

### Idempotency on Order Creation
Currently, a double-click on "Place Order" can create duplicate orders. The production fix: add an `idempotencyKey` field to `OrderRequest` (UUID generated client-side), store it in the `orders` table with a unique constraint, and return the existing order if the key already exists. This is a standard pattern for payment endpoints.

### Caching
No caching layer is implemented. For production, I would add:
- Redis for product catalog caching (products change infrequently, read-heavy workload)
- Spring `@Cacheable` on `getProduct()` and `getAllProducts()` with TTL-based eviction
- Cache invalidation on create/update/delete operations

### Observability Stack
Current logging uses SLF4J with structured messages. For production at scale:
- Micrometer + Prometheus for metrics collection
- Grafana dashboards for visualization
- OpenTelemetry for distributed tracing with correlation IDs across services
- Centralized log aggregation (ELK or Loki)

### Scalability Path
The current architecture is a well-structured monolith. To scale:
- **Horizontal scaling:** The stateless backend supports multiple instances behind a load balancer. Cart state lives in localStorage (client-side), and orders are persisted immediately — no server-side session affinity required.
- **Database scaling:** Read replicas for product queries, connection pooling with HikariCP (already configured).
- **Service extraction:** The payment adapters are already behind a port interface. Extracting them into a separate payment microservice is a clean cut — implement the same `PaymentPort` interface as a REST client instead of a local bean. At that point, the Saga pattern and distributed tracing become necessary.
- **Event-driven:** Order creation could publish events (Kafka/RabbitMQ) for inventory updates, email notifications, and analytics — decoupling the write path from downstream consumers.

### Other Production Considerations
- **JSON stability:** Replace raw `Page<T>` responses with a custom `PagedResponse<T>` DTO to guarantee stable JSON structure across Spring Data version upgrades.
- **Product images:** Add an `imageUrl` field to products, stored in S3/CDN. The frontend currently uses category-colored avatars as placeholders — the architecture supports the switch with minimal changes.
- **Database migrations for zero-downtime deploys:** All Flyway migrations should be backward-compatible (add columns with defaults, never drop columns in the same release).

## Project Structure

```
loanpro-ecommerce/
├── docker-compose.yml              ← Full stack: DB + Flyway + Backend + Frontend
├── .env                            ← Database credentials (gitignored)
├── README.md
├── CLAUDE.md                       ← AI assistant context file
├── db/data/Initial_data.csv        ← Seed data (96 products)
├── backend/
│   ├── Dockerfile                  ← Multi-stage: Maven build → JRE Alpine
│   ├── pom.xml
│   └── src/
│       ├── main/java/              ← Application code (Hexagonal Architecture)
│       ├── main/resources/
│       │   ├── application.yml     ← Env vars for Docker/production
│       │   ├── application-local.yml ← Local dev credentials (gitignored)
│       │   ├── application-docker.yml ← Docker profile
│       │   └── db/migration/       ← Flyway migrations (V1-V3)
│       └── test/java/              ← Unit tests + Karate integration tests
└── frontend/
    ├── Dockerfile                  ← Multi-stage: Node build → nginx Alpine
    ├── nginx.conf                  ← Proxies /api to backend, serves SPA
    └── src/
        ├── api/                    ← Axios client
        ├── components/             ← Products, Store, Cart components
        ├── context/                ← CartContext (localStorage-persisted)
        └── pages/                  ← ProductsPage, StorePage, CheckoutPage
```

## Git Strategy

Development followed a feature-branch workflow: `main` ← `develop` ← `feature/*` branches, with pull requests for every merge.

Feature branches (in development order): `db-setup`, `product-crud`, `csv-import`, `purchase-flow`, `frontend` (parts 1 & 2), `unit-testing`, `dockerize`, `observability`.

## AI Disclosure

AI (Claude) was used throughout this project as an architectural advisor and code generation assistant. My role was to make every technical decision — architecture patterns, database design, error handling strategies, security considerations. The AI generated code from detailed specifications I wrote; I reviewed, tested, and fixed every output.
