# FinSight AI — Intelligent Financial Backend System

> A production-grade, event-driven financial backend built with Java 21 and Spring Boot 3.
> Goes beyond standard CRUD — monitors spending in real time, fires automated budget alerts
> via real Gmail emails, caches analytics with Redis, generates PDF reports, manages database
> schema with Flyway migrations, and provides AI-powered financial advice using Google Gemini
> through LangChain4j. Fully Dockerized and deployed on AWS Render (Web Service & PostgreSQL) and Upstash (Redis).

---

## 🌐 Live Demo

| | |
|---|---|
| **Swagger UI** | https://finsight-ai-ivto.onrender.com/swagger-ui/index.html|
| **Health Check** | https://finsight-ai-ivto.onrender.com/api/health |

> Deployed on Render (Web Service & PostgreSQL) and Upstash (Redis)

---

## Table of Contents

- [What is FinSight AI](#what-is-finsight-ai)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Database Migrations](#database-migrations)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run with Docker](#run-with-docker)
  - [Run Locally](#run-locally)
- [Environment Variables](#environment-variables)
- [API Documentation](#api-documentation)
- [Key Design Decisions](#key-design-decisions)
- [What I Learned](#what-i-learned)

---

## What is FinSight AI

Most expense trackers are passive — they store what you tell them and nothing more.
FinSight AI is different. It actively monitors your spending behavior and reacts.

The system continuously tracks category-wise budgets, fires warnings when you cross
80% of a limit, sends real HTML emails directly to your Gmail inbox, generates automated
monthly PDF reports, and answers financial questions using AI that has access to your
real transaction data — not generic advice.

Built as a learning project to simulate real-world enterprise backend engineering
using modern Java and Spring Boot.

---

## Features

**Authentication & Security**
- JWT-based stateless authentication
- Refresh token rotation with database persistence
- BCrypt password hashing
- Spring Security filter chain with custom JWT filter
- Ownership validation on all resources — prevents IDOR vulnerabilities

**Expense Engine**
- Full CRUD for financial transactions
- Filter by category
- Pagination and sorting
- BigDecimal for all monetary values — no floating point errors

**Budget System**
- Category-wise monthly budget limits
- Live utilization tracking — spent, remaining, percentage
- Unique constraint per user/category/month/year
- Alert status: SAFE / WARNING / EXCEEDED

**Event-Driven Budget Alerts with Real Email Delivery**
- Every expense addition automatically triggers a budget check
- Logs WARNING at 80% usage, EXCEEDED at 100%
- Sends formatted HTML email to user's Gmail inbox automatically
- Email delivery is async — expense response returns instantly, email sends in background
- If email fails, expense is never affected — side effects never break core operations
- Spring Scheduler runs a daily sweep across all active budgets at midnight

**Analytics Engine**
- Monthly total spending
- Category breakdown with transaction counts
- Daily average spend based on active expense days
- Highest spending category detection
- JPQL constructor expressions for direct DTO projection

**AI Financial Advisor**
- Powered by Google Gemini via LangChain4j
- Fetches user's real financial data before every query
- Injects spending context, budget status, and category breakdown into the prompt
- Returns personalized, data-driven advice — not generic responses

**Redis Caching**
- Analytics responses cached with 10-minute TTL
- Cache evicted automatically on expense create, update, delete
- Zero database queries on cache hits

**Database Migration with Flyway**
- All schema changes managed through versioned SQL migration files
- `ddl-auto` set to `validate` — Hibernate never modifies schema automatically
- Full migration history tracked in `flyway_schema_history` table
- Every environment runs identical schema — no drift between local and production
- Existing migrations are immutable — changes always go in new migration files

**PDF Reports**
- Monthly report generated in memory using iText7
- Includes summary, category breakdown table, budget performance
- Downloaded directly via API — no file storage needed

**Dockerized & Cloud Deployed**
- Multi-stage Dockerfile for minimal image size
- Docker Compose orchestrates Spring Boot, PostgreSQL, Redis
- Health checks ensure correct startup ordering
- Volumes for PostgreSQL data persistence
- Environment variable injection for all secrets
- Deployed live on Render (Free Tier) — Multi-stage Docker Architecture

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 17 |
| ORM | Hibernate / Spring Data JPA |
| DB Migrations | Flyway |
| Security | Spring Security 6 + JWT (jjwt 0.12) |
| Cache | Redis 7 |
| Email | Spring Boot Starter Mail + Gmail SMTP |
| AI | Google Gemini via LangChain4j 0.36 |
| PDF | iText7 |
| Scheduler | Spring Scheduler |
| Documentation | SpringDoc OpenAPI / Swagger UI |
| Build | Maven |
| Containerization | Docker + Docker Compose |
| Cloud/hosting | Render (App & DB) + Upstash (Redis) |

---

## Architecture

The project follows strict **Layered Architecture**. Each layer has one responsibility
and communicates only with the layer directly below it.

```
Client (Swagger / Frontend)
         │
         │  HTTP Request + JWT token
         ▼
┌─────────────────────────────┐
│   Spring Security Filter    │  JWT validation on every request
│   Chain + JwtAuthFilter     │  Sets authentication in SecurityContext
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│      Controller Layer       │  Receives HTTP, validates input (@Valid)
│  @RestController            │  Delegates to service, returns ResponseEntity
└─────────────────────────────┘
         │  DTOs only cross this boundary
         ▼
┌─────────────────────────────┐
│       Service Layer         │  All business logic lives here
│  @Service @Transactional    │  getCurrentUser() from SecurityContext
└─────────────────────────────┘
         │
         ├──────────────────── Redis Cache (@Cacheable / @CacheEvict)
         ├──────────────────── EmailService (@Async background thread)
         │
         ▼
┌─────────────────────────────┐
│     Repository Layer        │  Spring Data JPA + custom JPQL queries
│  @Repository                │  No business logic
└─────────────────────────────┘
         │
         ▼
┌─────────────────────────────┐
│       PostgreSQL            │  users, expenses, budgets, refresh_tokens
│   + Hibernate ORM           │  Schema managed by Flyway migrations
│   + Flyway                  │  Running in Docker container on Render (App & DB) + Upstash (Redis)
└─────────────────────────────┘

Cross-cutting:
├── GlobalExceptionHandler    (@RestControllerAdvice)
├── RequestLoggingFilter      (logs method, path, status, duration)
├── BudgetAlertScheduler      (daily cron + async email notifications)
└── AiAdvisorService          (fetches context → calls Gemini → returns advice)
```

**Complete request flow for `POST /api/expenses`:**

1. Request hits embedded Tomcat on port 8080
2. Spring Security FilterChainProxy intercepts
3. `JwtAuthFilter` extracts Bearer token from Authorization header
4. `JwtService` validates signature against secret key, extracts email
5. `CustomUserDetailsService` loads user from database
6. `UsernamePasswordAuthenticationToken` stored in `SecurityContextHolder`
7. `DispatcherServlet` routes to `ExpenseController.addExpense()`
8. Jackson deserializes request body to `ExpenseRequest` DTO
9. `@Valid` triggers bean validation — fails fast with 400 if invalid
10. `ExpenseService.addExpense()` reads current user from SecurityContext
11. `Expense` entity built via builder pattern, saved via `expenseRepository`
12. Hibernate generates INSERT SQL, executes against PostgreSQL container
13. Data persisted on disk via Docker volume
14. `@CacheEvict` clears stale analytics from Redis
15. `BudgetService.checkBudgetAlert()` fires — checks 80%/100% thresholds
16. If threshold crossed — `EmailService` called
17. `@Async` runs email delivery in background thread — main thread continues
18. Entity mapped to `ExpenseResponse` DTO
19. `ResponseEntity` wraps DTO with HTTP 201 CREATED
20. Jackson serializes DTO to JSON
21. Tomcat sends JSON response back to client instantly
22. Email arrives in user's Gmail inbox seconds later (async)

---

## Project Structure

```
src/main/java/com/finsight/finsight_ai/
├── FinsightAiApplication.java
├── ai/
│   └── FinancialAdvisorAi.java
├── config/
│   ├── AiConfig.java
│   ├── RedisConfig.java
│   ├── RequestLoggingConfig.java
│   └── SwaggerConfig.java
├── controller/
│   ├── AiAdvisorController.java
│   ├── AnalyticsController.java
│   ├── AuthController.java
│   ├── BudgetController.java
│   ├── ExpenseController.java
│   ├── HealthController.java
│   └── ReportController.java
├── dto/
│   ├── request/
│   │   ├── AiAdviceRequest.java
│   │   ├── BudgetRequest.java
│   │   ├── ExpenseRequest.java
│   │   ├── LoginRequest.java
│   │   └── RegisterRequest.java
│   └── response/
│       ├── AiAdviceResponse.java
│       ├── AuthResponse.java
│       ├── BudgetResponse.java
│       ├── CategorySpendResponse.java
│       ├── ErrorResponse.java
│       ├── ExpenseResponse.java
│       └── MonthlyAnalyticsResponse.java
├── entity/
│   ├── Budget.java
│   ├── Category.java
│   ├── Expense.java
│   ├── RefreshToken.java
│   └── User.java
├── exception/
│   ├── AccessDeniedException.java
│   ├── DuplicateResourceException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── repository/
│   ├── BudgetRepository.java
│   ├── ExpenseRepository.java
│   ├── RefreshTokenRepository.java
│   └── UserRepository.java
├── scheduler/
│   └── BudgetAlertScheduler.java
├── security/
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthFilter.java
│   └── JwtService.java
└── service/
    ├── AiAdvisorService.java
    ├── AnalyticsService.java
    ├── AuthService.java
    ├── BudgetService.java
    ├── EmailService.java
    ├── ExpenseService.java
    └── ReportService.java

src/main/resources/
├── application.yml
└── db/
    └── migration/
        ├── V1__create_users_table.sql
        ├── V2__create_refresh_tokens_table.sql
        ├── V3__create_expenses_table.sql
        ├── V4__create_budgets_table.sql
        └── V5__add_indexes.sql
```

---

## Database Migrations

Schema is managed by **Flyway** — not Hibernate auto-update.

Every database change is a versioned SQL file that runs exactly once, in order,
and is tracked in `flyway_schema_history`. This guarantees identical schema
across all environments — local, team members, and production server.

| Version | Description | Type |
|---|---|---|
| V1 | Create users table | Baseline |
| V2 | Create refresh_tokens table with FK | SQL |
| V3 | Create expenses table with FK | SQL |
| V4 | Create budgets table with unique constraint | SQL |
| V5 | Add indexes on user_id, category, expense_date | SQL |

**Rule:** Never modify an existing migration file. Always add a new one.
Flyway checksums every migration — any modification to an applied file
causes startup failure to protect schema integrity.

---

## Getting Started

### Prerequisites

- Docker Desktop installed and running
- Git
- A Gemini API key — https://aistudio.google.com/app/apikey (free tier)
- A Gmail account with App Password generated

Java, PostgreSQL, and Redis do not need to be installed locally.
Everything runs inside Docker.

### Run with Docker

**1. Clone the repository**

```bash
git clone https://github.com/your-username/finsight-ai.git
cd finsight-ai
```

**2. Create a `.env` file in the project root**

```env
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
MAIL_USERNAME=your.gmail@gmail.com
MAIL_PASSWORD=your_16_char_app_password
GEMINI_API_KEY=your_gemini_api_key_here
APPLICATION_JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
APPLICATION_JWT_EXPIRATION=900000
APPLICATION_JWT_REFRESH_EXPIRATION=604800000
```

> Gmail App Password: Google Account → Security → 2-Step Verification → App Passwords

**3. Start all containers**

```bash
docker-compose up --build
```

Wait for:
```
finsight-postgres  | database system is ready to accept connections
finsight-redis     | Ready to accept connections
finsight-app       | Started FinsightAiApplication in X seconds
```

**4. Open Swagger UI**

```
http://localhost:8080/swagger-ui/index.html
```

**5. Stop everything**

```bash
# Stop containers
docker-compose down

# Stop and wipe database
docker-compose down -v
```

---

### Run Locally

**Prerequisites:** Java 21, Maven, PostgreSQL 17, Redis 7

**1. Create the database**

```sql
CREATE DATABASE finsight_db;
```

**2. Set environment variables**

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finsight_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_password
export SPRING_DATA_REDIS_HOST=localhost
export MAIL_USERNAME=your.gmail@gmail.com
export MAIL_PASSWORD=your_app_password
export GEMINI_API_KEY=your_gemini_api_key
export APPLICATION_JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

**3. Run**

```bash
mvn spring-boot:run
```

---

## Environment Variables

| Variable | Description | Required |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | Yes |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | Yes |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | Yes |
| `SPRING_DATA_REDIS_HOST` | Redis hostname | Yes |
| `APPLICATION_JWT_SECRET` | 256-bit hex secret for JWT signing | Yes |
| `APPLICATION_JWT_EXPIRATION` | Access token TTL in ms — default 900000 (15 min) | Yes |
| `APPLICATION_JWT_REFRESH_EXPIRATION` | Refresh token TTL in ms — default 604800000 (7 days) | Yes |
| `GEMINI_API_KEY` | Google Gemini API key | Yes |
| `MAIL_USERNAME` | Gmail address used for sending alerts | Yes |
| `MAIL_PASSWORD` | Gmail App Password — 16 characters | Yes |

> Never commit `.env` to version control. It is in `.gitignore`.

---

## API Documentation

Full interactive documentation: `/swagger-ui/index.html`

Use the **Authorize** button to set your JWT token for all protected endpoints.

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/login` | No | Login, receive tokens |

**Register request:**
```json
{
  "name": "Purushottam",
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "a3f2e1b4-...",
  "email": "user@example.com",
  "name": "Purushottam"
}
```

---

### Expenses

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/expenses` | Yes | Add new expense |
| GET | `/api/expenses` | Yes | Get paginated expenses |
| GET | `/api/expenses?category=FOOD` | Yes | Filter by category |
| PUT | `/api/expenses/{id}` | Yes | Update expense |
| DELETE | `/api/expenses/{id}` | Yes | Delete expense — returns 204 |

Available categories: `FOOD` `TRANSPORT` `ENTERTAINMENT` `SHOPPING`
`HEALTHCARE` `UTILITIES` `EDUCATION` `OTHER`

**Add expense request:**
```json
{
  "amount": 450.00,
  "description": "Lunch with team",
  "category": "FOOD",
  "expenseDate": "2026-05-24"
}
```

Paginated response includes: `content`, `totalElements`, `totalPages`, `number`, `size`

---

### Budgets

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/budgets` | Yes | Create monthly category budget |
| GET | `/api/budgets` | Yes | Get budgets for current month |
| GET | `/api/budgets?month=5&year=2026` | Yes | Specific month |

**Budget response with live utilization:**
```json
{
  "id": 1,
  "category": "FOOD",
  "limitAmount": 1500.00,
  "spentAmount": 1200.00,
  "remainingAmount": 300.00,
  "usagePercentage": 80.0,
  "month": 5,
  "year": 2026,
  "alertStatus": "WARNING"
}
```

When `usagePercentage` crosses 80% — warning email sent to user's Gmail.
When it crosses 100% — budget exceeded email sent.

---

### Analytics

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/analytics/monthly` | Yes | Current month analytics |
| GET | `/api/analytics/monthly?month=5&year=2026` | Yes | Specific month |

**Response:**
```json
{
  "month": 5,
  "year": 2026,
  "totalSpent": 36659.10,
  "dailyAverage": 1833.00,
  "highestSpendingCategory": "EDUCATION",
  "categoryBreakdown": [
    {
      "category": "EDUCATION",
      "totalAmount": 33650.00,
      "transactionCount": 2
    },
    {
      "category": "FOOD",
      "totalAmount": 845.10,
      "transactionCount": 3
    }
  ]
}
```

Results cached in Redis for 10 minutes. Cache invalidated on any expense change.

---

### AI Advisor

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/ai/advice` | Yes | Get personalized financial advice |

**Request:**
```json
{
  "question": "Am I on track with my budget this month?"
}
```

**Real response from Gemini:**
```json
{
  "question": "Am I on track with my budget this month?",
  "advice": "You've spent a total of ₹36659.10 this month, with a substantial
             ₹33650.00 allocated to EDUCATION. While your TRANSPORT budget is
             safely at 44.0% spent, your FOOD budget is nearing its limit at
             84.5% (₹845.10 of ₹1000.00), so you'll need to be mindful there.
             Keep a close eye on your food spending to stay within that budget.",
  "timestamp": "2026-05-21T16:58:32"
}
```

Every response references the user's actual spending numbers — not generic advice.
The AI receives real category breakdown, budget status, and totals before answering.

---

### Reports

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/reports/monthly` | Yes | Download current month PDF |
| GET | `/api/reports/monthly?month=5&year=2026` | Yes | Specific month PDF |

Returns a formatted PDF file download. Includes spending summary,
category breakdown table, and budget performance.
Generated entirely in memory — no server-side file storage required.

---

### Health Check

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/health` | No | System health and DB status |

```json
{
  "status": "UP",
  "service": "FinSight AI",
  "version": "1.0",
  "timestamp": "2026-05-24T10:00:00",
  "database": "UP"
}
```

---

## Key Design Decisions

**Why JWT over sessions?**
JWT is stateless — the server stores nothing per user. Any server instance
can validate any token independently. Horizontally scalable with zero shared state.

**Why two tokens (access + refresh)?**
Access tokens expire in 15 minutes — limited damage if stolen. Refresh tokens
(7 days, stored in DB) allow silent renewal and can be revoked by deleting
the database row — something impossible with a stateless JWT.

**Why Flyway over `ddl-auto: update`?**
`ddl-auto: update` never deletes columns, has no history, and causes schema drift
between environments. Flyway gives every change a version number, a checksum,
and a timestamp. Every environment runs identical SQL in identical order.
Flyway refuses to start if an applied migration is modified — protecting
schema history from silent corruption.

**Why `@Async` for email alerts?**
Gmail SMTP calls take 1-3 seconds. Without `@Async`, every expense addition
would block the response thread waiting for email delivery. With `@Async`,
the expense saves and responds in milliseconds while email delivers in a
background thread. Email failure never rolls back a successful expense save —
side effects never break core operations.

**Why BigDecimal for money?**
Floating point cannot represent 0.1 exactly in binary.
`0.1 + 0.2 = 0.30000000000000004` in IEEE 754. For financial data this is
unacceptable. `BigDecimal` with `DECIMAL(10,2)` in PostgreSQL guarantees
exact arithmetic.

**Why DTOs instead of exposing entities?**
Entities are database representations. DTOs are API contracts. Keeping them
separate means database schema changes don't break the API. DTOs also prevent
mass assignment — a client cannot inject `role: ADMIN` if the request DTO
has no role field.

**Why format AI context as strings not raw JSON?**
LLMs are trained on human language. `"FOOD: ₹4200 of ₹5000 (84%) — WARNING"`
is more natural than a raw JSON object. This also reduces token usage —
no field names, brackets, or structural overhead. Better accuracy,
lower cost, faster responses.

**Why Redis cache with eviction instead of longer TTL?**
Analytics change every time an expense is added. Eviction-on-write gives
the performance benefit of caching with the correctness of always-fresh data.

**Why EC2 over serverless?**
The app is Dockerized with a full stack. EC2 runs the exact same
`docker-compose up` as local development — zero rewriting, zero new
deployment paradigms. Simpler, faster, and demonstrates real Docker skills.

---

## What I Learned

Building FinSight AI covered the full spectrum of backend engineering:

- **Layered architecture** — strict separation between controllers, services,
  repositories, DTOs, and entities
- **JWT internals** — header, payload, signature, stateless auth, token rotation
- **Spring Security filter chain** — how every request flows through filters
  before reaching controllers
- **JPA and Hibernate** — entity relationships, lazy loading, JPQL, constructor
  projections, transaction management
- **Flyway migrations** — versioned schema management, why immutable history
  matters, the difference between validate and update
- **Event-driven design** — expense creation triggering budget checks without
  tight coupling between services
- **Async processing** — `@Async` for non-blocking side effects, why email
  delivery must never block core operations
- **Redis caching** — cache-aside pattern, TTL, invalidation on write
- **Docker networking** — why containers use service names not localhost,
  health checks, volume persistence
- **Render (App & DB) + Upstash (Redis)**
- **AI integration** — prompt engineering, context injection, LangChain4j
  service interfaces
- **Production practices** — graceful shutdown, structured logging,
  global exception handling, environment variable configuration,
  Gmail SMTP with App Passwords

---

## Author

**Purushottam Patel**
Backend Engineer

GitHub: https://github.com/ppatel-tech

---

*Built with Java 21 · Spring Boot 3 · PostgreSQL · Redis · Flyway · Docker · AWS EC2 · LangChain4j · Google Gemini*
