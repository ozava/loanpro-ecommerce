# CLAUDE.md — Project Context for AI Assistants

## Project Overview
Enterprise-grade e-commerce application built as a code challenge for LoanPro. The challenge evaluates architecture decisions, code quality, and the ability to guide AI effectively.

## Tech Stack
- **Backend:** Java 17, Spring Boot 3.4.x (3.5.15), Maven
- **Database:** PostgreSQL 16 (Alpine), managed via Flyway migrations
- **ORM:** Spring Data JPA (Hibernate 6.6.x) with `ddl-auto: validate`
- **Frontend:** React (pending)
- **Containerization:** Docker Compose
- **IDE:** IntelliJ IDEA with Claude Agent

## Architecture
Hexagonal Architecture with clear layer separation:
```
com.loanpro.ecommerce/
├── domain/
│   ├── entity/          → Category, Product (JPA entities)
│   └── repository/      → CategoryRepository, ProductRepository
├── application/
│   ├── dto/             → ProductRequest, ProductResponse, CsvImportResult
│   ├── service/         → ProductService, CsvImportService, ProductBatchSaver
│   └── exception/       → ResourceNotFoundException, DuplicateSkuException, InvalidCsvException
└── infrastructure/
    └── web/             → ProductController, GlobalExceptionHandler
```

## Project Structure
```
loanpro-ecommerce/               ← GitHub repo root
├── docker-compose.yml           ← PostgreSQL + Flyway services
├── .env                         ← DB credentials (in .gitignore)
├── .gitignore
├── README.md
├── CLAUDE.md                    ← this file
├── db/
│   └── data/
│       └── Initial_data.csv     ← seed CSV (96 products)
├── backend/                     ← Spring Boot project (opened as separate project in IntelliJ)
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/
│       └── main/
│           ├── java/com/loanpro/ecommerce/
│           │   ├── EcommerceApplication.java (@EnableScheduling)
│           │   ├── domain/
│           │   ├── application/
│           │   └── infrastructure/
│           └── resources/
│               ├── application.yml            ← uses ${POSTGRES_*} env vars, flyway.enabled: false
│               ├── application-local.yml      ← hardcoded credentials for local dev (in .gitignore)
│               └── db/
│                   └── migration/
│                       ├── V1__schema.sql     ← tables, indexes, constraints
│                       └── V2__seed_data.sql  ← loads Initial_data.csv via staging table
└── frontend/                    ← React app (pending)
```

## Database Schema
```sql
-- All PKs are BIGSERIAL, all FKs are BIGINT (required by Hibernate 6)
categories (id BIGSERIAL PK, name VARCHAR(100) UNIQUE)
products (id BIGSERIAL PK, sku VARCHAR(50) UNIQUE, name VARCHAR(200), description TEXT,
          category_id BIGINT FK→categories, price NUMERIC(10,2) CHECK>=0,
          stock INTEGER CHECK>=0, weight_kg NUMERIC(8,3),
          created_at TIMESTAMP, updated_at TIMESTAMP)
orders (id BIGSERIAL PK, status VARCHAR(20) DEFAULT 'completed',
        total_amount NUMERIC(12,2), created_at TIMESTAMP)
order_items (id BIGSERIAL PK, order_id BIGINT FK→orders ON DELETE CASCADE,
             product_id BIGINT FK→products, quantity INTEGER CHECK>0,
             unit_price NUMERIC(10,2), subtotal NUMERIC(12,2))
```
Indexes: GIN on products(name || description) for full-text search, B-tree on sku and category_id.

## Docker Setup
- PostgreSQL runs on port `5433:5432` (5433 external to avoid conflicts with local PG)
- Flyway runs as a separate container, reads migrations from `./backend/src/main/resources/db/migration` mounted at `/flyway/migrations`
- CSV seed data mounted into the `db` container at `/flyway/data/` (because `COPY` runs server-side)
- Flyway command uses `-locations=filesystem:/flyway/migrations`
- Spring Boot's Flyway is disabled (`flyway.enabled: false`) — Docker Flyway handles migrations

## Implemented Features

### 1. Product CRUD (feature/product-crud — merged to develop)
- `GET /api/products` — list all
- `GET /api/products/{id}` — get by ID
- `POST /api/products` — create (with @Valid)
- `PUT /api/products/{id}` — update (with SKU uniqueness check → 409)
- `DELETE /api/products/{id}` — delete (204 No Content)
- `GET /api/products/search?q=` — search by name or description

### 2. CSV Import (feature/csv-import — merged to develop)
- `POST /api/products/import` — upload CSV, returns JSON summary + errorFileId
- `GET /api/products/import/errors/{errorFileId}` — download error CSV (expires 10min)
- Exhaustive validation per field (name, sku, description, category, price, stock, weight_kg)
- Tolerant to dirty data: strips `$` from prices, trims whitespace, auto-creates categories
- Multiple errors per row accumulated and reported together
- Batch inserts of 50 products per transaction (REQUIRES_NEW) with EntityManager.clear()
- Pre-loads all SKUs and categories before loop (prevents N+1)
- File size limit: 10MB
- Performance: 1,005 rows processed in 2.84s

### 3. Error Handling
- `ResourceNotFoundException` → 404
- `DuplicateSkuException` → 409 Conflict
- `InvalidCsvException` → 400 Bad Request
- `MethodArgumentNotValidException` → 400 with field-level errors
- `MaxUploadSizeExceededException` → 413 Payload Too Large

## Pending Features
- [ ] **Purchase flow** — `POST /api/orders` to simulate purchases (orders + order_items, fake payment, stock deduction)
- [ ] **Frontend** — React UI for CRUD, search, CSV import, and purchase
- [ ] **Dockerize backend** — Dockerfile with multi-stage build (Maven build → JRE-only runtime)
- [ ] **Full docker-compose** — orchestrate db + backend + frontend
- [ ] **README.md** — architecture decisions, how to run, alternatives considered

## Git Strategy
- `main` ← only stable code via PRs from `develop`
- `develop` ← integration branch
- `feature/*` ← individual features, PR into `develop`
- Completed: `feature/db-setup`, `feature/product-crud`, `feature/csv-import`
- Next: `feature/purchase-flow`

## Commands Cheat Sheet
```bash
# Start DB + Flyway
docker compose up -d
docker logs loanpro-flyway

# Reset DB (nuclear option)
docker compose down -v
docker compose up -d

# Run backend locally
cd backend
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=local"

# Connect DBeaver: localhost:5433, loanpro_ecommerce, loanpro_user, loanpro_pass123
```

## Key Decisions
1. **Flyway in Docker, not Spring Boot** — avoids dual migration execution conflicts
2. **BIGSERIAL over SERIAL** — Hibernate 6 maps Long to BIGINT, must match
3. **ddl-auto: validate** — Hibernate never modifies schema, only validates entity mapping
4. **JPA for CRUD + JdbcTemplate for batch imports** — best of both worlds
5. **ProductBatchSaver as separate @Service** — required for Spring AOP proxy with REQUIRES_NEW
6. **application-local.yml** — local dev credentials, gitignored; application.yml uses env vars for Docker/production
7. **No external CSV libraries** — manual parser with quoted field support, Java standard only
