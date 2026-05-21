# FinSight AI — Intelligent Financial Backend System

> A production-grade, event-driven financial backend built with Java 21 and Spring Boot 3.
> Goes beyond standard CRUD — monitors spending in real time, fires automated budget alerts,
> caches analytics with Redis, generates PDF reports, and provides AI-powered financial advice
> using Google Gemini through LangChain4j.

---

## Table of Contents

- [What is FinSight AI](#what-is-finsight-ai)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run with Docker](#run-with-docker)
  - [Run Locally](#run-locally)
- [Environment Variables](#environment-variables)
- [API Documentation](#api-documentation)
  - [Authentication](#authentication)
  - [Expenses](#expenses)
  - [Budgets](#budgets)
  - [Analytics](#analytics)
  - [AI Advisor](#ai-advisor)
  - [Reports](#reports)
- [Key Design Decisions](#key-design-decisions)
- [What I Learned](#what-i-learned)

---

## What is FinSight AI

Most expense trackers are passive — they store what you tell them and nothing more.
FinSight AI is different. It actively monitors your spending behavior and reacts.

The system continuously tracks category-wise budgets, fires warnings when you cross
80% of a limit, generates automated monthly PDF reports, and answers financial questions
using AI that has access to your real transaction data — not generic advice.

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

**Event-Driven Budget Alerts**
- Every expense addition automatically triggers a budget check
- Logs WARNING at 80% usage, EXCEEDED at 100%
- Spring Scheduler runs a daily sweep across all active budgets

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

**PDF Reports**
- Monthly report generated in memory using iText7
- Includes summary, category breakdown table, budget performance
- Downloaded directly via API — no file storage needed

**Scheduler**
- Daily cron job checks all active budgets at midnight
- Fixed-rate job for development testing
- Uses fixedDelay for sequential execution — no overlapping runs

**Dockerized**
- Multi-stage Dockerfile for minimal image size
- Docker Compose orchestrates Spring Boot, PostgreSQL, Redis
- Health checks ensure correct startup ordering
- Volumes for PostgreSQL data persistence
- Environment variable injection for all secrets

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 17 |
| ORM | Hibernate / Spring Data JPA |
| Security | Spring Security 6 + JWT (jjwt 0.12) |
| Cache | Redis 7 |
| AI | Google Gemini via LangChain4j 0.36 |
| PDF | iText7 |
| Scheduler | Spring Scheduler |
| Documentation | SpringDoc OpenAPI / Swagger UI |
| Build | Maven |
| Containerization | Docker + Docker Compose |

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
│   + Hibernate ORM           │  Running in Docker container
└─────────────────────────────┘

Cross-cutting:
├── GlobalExceptionHandler    (@RestControllerAdvice)
├── RequestLoggingFilter      (logs method, path, status, duration)
├── BudgetAlertScheduler      (daily cron + fixed-rate background jobs)
└── AiAdvisorService          (fetches context → calls Gemini → returns advice)
```

**Request flow for `POST /api/expenses`:**

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
15. `BudgetService.checkBudgetAlert()` fires — logs WARNING or EXCEEDED if threshold crossed
16. Entity mapped to `ExpenseResponse` DTO
17. `ResponseEntity` wraps DTO with HTTP 201 CREATED
18. Jackson serializes DTO to JSON
19. Tomcat sends JSON response back to client

---

## Project Structure

```
src/main/java/com/finsight/finsight_ai/
├── FinsightAiApplication.java
├── ai/
│   └── FinancialAdvisorAi.java          LangChain4j AI service interface
├── config/
│   ├── AiConfig.java                    Gemini + LangChain4j bean setup
│   ├── RedisConfig.java                 Cache manager, TTL, JSON serializer
│   ├── RequestLoggingConfig.java        Request/response logging filter
│   └── SwaggerConfig.java              OpenAPI + JWT bearer auth setup
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
    ├── ExpenseService.java
    └── ReportService.java
```

---

## Getting Started

### Prerequisites

- Docker Desktop installed and running
- Git
- A Gemini API key from https://aistudio.google.com/app/apikey (free)

That's it. Java, PostgreSQL, and Redis do not need to be installed locally.
Everything runs inside Docker.

### Run with Docker

**1. Clone the repository**

```bash
git clone https://github.com/your-username/finsight-ai.git
cd finsight-ai
```

**2. Create a `.env` file in the project root**

```env
GEMINI_API_KEY=your_gemini_api_key_here
```

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
docker-compose down
```

To also wipe the database:
```bash
docker-compose down -v
```

---

### Run Locally

If you prefer to run without Docker:

**Prerequisites:** Java 21, Maven, PostgreSQL 17, Redis 7

**1. Create the database**

```sql
CREATE DATABASE finsight_db;
```

**2. Set environment variables** (or update `application.yml` directly for dev)

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/finsight_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_password
export SPRING_DATA_REDIS_HOST=localhost
export GEMINI_API_KEY=your_gemini_api_key
export APPLICATION_JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

**3. Run**

```bash
mvn spring-boot:run
```

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/finsight_db` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password | `postgres` |
| `SPRING_DATA_REDIS_HOST` | Redis hostname | `localhost` |
| `APPLICATION_JWT_SECRET` | 256-bit hex secret for JWT signing | (see yml) |
| `APPLICATION_JWT_EXPIRATION` | Access token TTL in ms | `900000` (15 min) |
| `APPLICATION_JWT_REFRESH_EXPIRATION` | Refresh token TTL in ms | `604800000` (7 days) |
| `GEMINI_API_KEY` | Google Gemini API key | required |

---

## API Documentation

Full interactive documentation available at `/swagger-ui/index.html`.
Use the **Authorize** button to set your JWT token for protected endpoints.

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

**Login / Register response:**
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
| DELETE | `/api/expenses/{id}` | Yes | Delete expense |

**Add expense request:**
```json
{
  "amount": 450.00,
  "description": "Lunch with team",
  "category": "FOOD",
  "expenseDate": "2026-05-21"
}
```

Available categories: `FOOD`, `TRANSPORT`, `ENTERTAINMENT`, `SHOPPING`,
`HEALTHCARE`, `UTILITIES`, `EDUCATION`, `OTHER`

**Paginated response includes:** `content`, `totalElements`, `totalPages`,
`number`, `size`

---

### Budgets

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/budgets` | Yes | Create monthly budget |
| GET | `/api/budgets` | Yes | Get budgets for current month |
| GET | `/api/budgets?month=5&year=2026` | Yes | Get budgets for specific month |

**Create budget request:**
```json
{
  "category": "FOOD",
  "limitAmount": 5000.00,
  "month": 5,
  "year": 2026
}
```

**Budget response includes live utilization:**
```json
{
  "id": 1,
  "category": "FOOD",
  "limitAmount": 5000.00,
  "spentAmount": 4200.00,
  "remainingAmount": 800.00,
  "usagePercentage": 84.0,
  "month": 5,
  "year": 2026,
  "alertStatus": "WARNING"
}
```

---

### Analytics

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/analytics/monthly` | Yes | Monthly spending analytics |
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

Results are cached in Redis for 10 minutes. Cache is invalidated automatically
on any expense change.

---

### AI Advisor

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/ai/advice` | Yes | Get AI financial advice |

**Request:**
```json
{
  "question": "Am I on track with my budget this month?"
}
```

**Response:**
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

The AI receives your actual spending data, budget status, and category breakdown
before generating a response. Every answer references your real numbers.

---

### Reports

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/reports/monthly` | Yes | Download monthly PDF report |
| GET | `/api/reports/monthly?month=5&year=2026` | Yes | Specific month PDF |

Returns a PDF file download. Report includes total spending summary,
category breakdown table, and budget performance.

---

### Health Check

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/health` | No | System health + DB status |

```json
{
  "status": "UP",
  "service": "FinSight AI",
  "version": "1.0",
  "timestamp": "2026-05-21T10:00:00",
  "database": "UP"
}
```

---

## Key Design Decisions

**Why JWT over sessions?**
JWT is stateless — the server stores nothing per user. Any server instance can
validate any token independently. This makes the system horizontally scalable
with zero shared session state.

**Why two tokens (access + refresh)?**
Access tokens expire in 15 minutes — short enough that a stolen token has
limited usefulness. But forcing re-login every 15 minutes is bad UX.
Refresh tokens (7 days, stored in DB) allow silent renewal. They can also be
revoked by deleting the database row — something you cannot do with a JWT.

**Why BigDecimal for money?**
Floating point arithmetic cannot represent 0.1 exactly in binary.
`0.1 + 0.2 = 0.30000000000000004` in IEEE 754. For financial data,
this is unacceptable. `BigDecimal` with `DECIMAL(10,2)` in PostgreSQL
guarantees exact arithmetic.

**Why DTOs instead of exposing entities?**
Entities are database representations. APIs are contracts with clients.
Mixing them means a database schema change breaks your API.
DTOs also prevent mass assignment vulnerabilities — a client cannot
inject `role: ADMIN` if your request DTO has no role field.

**Why Redis cache with eviction instead of longer TTL?**
Analytics data changes every time an expense is added. A long TTL
means users see stale totals. Eviction-on-write gives you the performance
benefit of caching with the correctness of always-fresh data.

**Why format context as strings for the AI prompt?**
LLMs are trained on human language, not JSON structure. Formatted strings
like `"FOOD: ₹4200 of ₹5000 (84%) — WARNING"` are more natural than raw
JSON objects. This also reduces token usage — no field names, brackets,
or structural overhead. Better accuracy, lower cost, faster responses.

---

## What I Learned

Building FinSight AI covered the full spectrum of backend engineering:

- **Layered architecture** — strict separation between controllers, services,
  repositories, DTOs, and entities
- **JWT internals** — header, payload, signature, stateless auth, token rotation
- **Spring Security filter chain** — how requests flow through filters before
  reaching controllers
- **JPA and Hibernate** — entity relationships, lazy loading, JPQL, constructor
  projections, transaction management
- **Event-driven design** — expense creation triggering budget checks without
  tight coupling
- **Redis caching** — cache-aside pattern, TTL, invalidation on write
- **Docker networking** — why containers use service names not localhost,
  health checks, volume persistence
- **AI integration** — prompt engineering, context injection, LangChain4j
  service interfaces
- **Production practices** — graceful shutdown, structured logging,
  global exception handling, environment variable configuration

---

## Author

**Purushottam Patel**
Backend Engineer in Training

---

*Built with Java 21 · Spring Boot 3 · PostgreSQL · Redis · Docker · LangChain4j · Google Gemini*
