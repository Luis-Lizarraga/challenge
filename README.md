# 🛒 E-Commerce Cart Management API

REST API for shopping cart and checkout management, developed with
**Java 21** and **Spring Boot 3.3.4**.

The project is designed as a backend engineering exercise focused not
only on CRUD operations, but also on **concurrency control, data
integrity, asynchronous order processing, JWT security, stress testing,
automated testing and observability**.

It includes unit/integration tests, Postman functional tests, JMeter
stress/concurrency plans, Docker support, Swagger/OpenAPI documentation,
Prometheus metrics, Grafana dashboards and centralized logging with
Loki + Promtail.

------------------------------------------------------------------------

## 📌 Main goals

The implementation focuses on several problems commonly found in
transactional backend systems:

-   Prevent lost updates when multiple requests modify the same cart.
-   Prevent duplicate products inside the same cart.
-   Prevent stock from becoming negative when different carts purchase
    concurrently.
-   Restrict cart access to the authenticated owner.
-   Process checkout asynchronously without blocking the HTTP request.
-   Make checkout resilient to concurrent processing conflicts.
-   Expose application/JVM metrics and centralized logs.
-   Validate behavior with automated, concurrency and stress tests.

------------------------------------------------------------------------

## 🧱 Architecture

The application follows a layered structure:

``` text
HTTP Request
     │
     ▼
Controller
     │
     ▼
Service / Domain
     │
     ├──────────────► Discount Strategy / Cache
     │
     ▼
Repository
     │
     ▼
Database

Checkout
     │
     ▼
Transactional Event
     │ AFTER_COMMIT
     ▼
Async OrderProcessingListener
     │
     ├──────────────► StockService
     │
     ▼
Database
```

Main packages:

``` text
com.lucho.tienda
├── config          # Async, cache, OpenAPI and application configuration
├── controller      # REST endpoints
├── dto             # API request/response models
├── event           # Application events
├── exception       # Business exceptions and global error handling
├── listener        # Asynchronous checkout processing
├── model           # JPA entities and domain behavior
├── repository      # Spring Data JPA repositories
├── security        # JWT authentication and Spring Security
├── service         # Application/business services
└── strategy        # Discount strategy implementation
```

------------------------------------------------------------------------

## 🛠️ Technology stack

  Area                     Technology
  ------------------------ ---------------------------------------------------
  Language                 Java 21
  Framework                Spring Boot 3.3.4
  REST                     Spring Web MVC
  Persistence              Spring Data JPA / Hibernate
  Security                 Spring Security + JWT (JJWT 0.12.6)
  Validation               Jakarta Bean Validation
  Development DB           H2
  Production DB            External datasource through environment variables
  Cache                    Spring Cache + Caffeine
  Resilience               Spring Retry
  Async processing         Spring `@Async` + transactional events
  API documentation        Springdoc OpenAPI / Swagger UI
  Metrics                  Spring Boot Actuator + Micrometer + Prometheus
  Dashboards               Grafana
  Logs                     Loki + Promtail
  Unit/integration tests   JUnit 5, Mockito, Spring Security Test
  API testing              Postman
  Load/stress testing      Apache JMeter 5.6.3
  Containers               Docker + Docker Compose

------------------------------------------------------------------------

## 🔐 Authentication and authorization

The API uses stateless JWT authentication.

Authentication flow:

``` text
POST /api/auth/login
        │
        ▼
Credentials validated
        │
        ▼
JWT generated
(username + userId + role)
        │
        ▼
Authorization: Bearer <token>
        │
        ▼
JwtAuthenticationFilter
        │
        ▼
SecurityContext
```

The JWT filter reconstructs the authenticated `UserDetailsImpl` directly
from validated claims, avoiding an additional user lookup on every
authenticated request.

Security characteristics:

-   Stateless sessions (`SessionCreationPolicy.STATELESS`).
-   JWT signature and expiration validation.
-   Role information is preserved in the authenticated principal.
-   Missing security claims do not silently receive a default role.
-   `/api/auth/**`, Swagger, health and Prometheus endpoints are public.
-   Other application endpoints require authentication.
-   Additional Actuator endpoints require `ROLE_ADMIN`.
-   The H2 console is enabled for the development profile.

### Cart ownership

User-facing cart operations are scoped by both:

``` text
cartId + authenticated userId
```

Repository queries enforce ownership instead of loading arbitrary carts
and validating them only afterwards.

Example:

``` sql
WHERE cart.id = :cartId
  AND cart.user.id = :userId
```

This prevents an authenticated user from reading or modifying another
user's cart simply by changing the cart ID in the URL.

------------------------------------------------------------------------

## 🔒 Concurrency and data integrity

Concurrency is one of the main areas covered by this project.

### Cart modifications --- pessimistic locking

Mutating operations on a cart use a database `PESSIMISTIC_WRITE` lock.

``` text
Request A ─┐
Request B ─┤
Request C ─┼──► Same Cart
           │
           ▼
     PESSIMISTIC_WRITE
           │
           ▼
   Serialized changes
```

This prevents concurrent requests from reading the same stale cart state
and attempting duplicate inserts or overwriting each other's quantity
changes.

The database also enforces:

``` text
UNIQUE(cart_id, product_id)
```

so a cart cannot contain duplicate rows for the same product.

### Optimistic versioning

`Cart` also contains a JPA `@Version` field. It provides version-based
protection for flows where optimistic concurrency detection is useful,
including asynchronous processing.

### Atomic stock deduction

Stock is not handled with a vulnerable read-then-write sequence.

Instead, the repository performs a conditional atomic update:

``` sql
UPDATE Product
SET stock = stock - :quantity
WHERE code = :code
  AND stock >= :quantity
```

The number of affected rows determines whether the operation succeeded.

This protects inventory when **different carts attempt to purchase the
same product concurrently** and prevents negative stock.

### Retry and recovery

Asynchronous order processing uses Spring Retry for optimistic
concurrency conflicts.

If retries are exhausted, recovery logic prevents the cart from
remaining indefinitely in `PROCESSING`.

------------------------------------------------------------------------

## 🛍️ Cart lifecycle

The cart exposes explicit domain transitions instead of allowing
checkout state changes to be scattered throughout the application.

Simplified lifecycle:

``` text
CREATED
   │
   │ checkout
   ▼
PROCESSING
   │
   ├────────────► PROCESSED
   │
   ├────────────► CANCELLED
   │
   └────────────► FAILED
```

Checkout initiation validates the current state before transitioning to
`PROCESSING`.

The asynchronous processor later completes the order or moves it to an
error/cancellation state according to the business result.

------------------------------------------------------------------------

## ⚡ Asynchronous checkout

Checkout is intentionally separated into two phases.

### 1. HTTP transaction

``` text
POST /api/carts/{cartId}/process
        │
        ▼
Lock + validate cart
        │
        ▼
CREATED → PROCESSING
        │
        ▼
Publish OrderProcessingEvent
        │
        ▼
COMMIT
        │
        ▼
HTTP 202 Accepted
```

### 2. Background processing

The event is consumed with:

-   `@TransactionalEventListener(phase = AFTER_COMMIT)`
-   `@Async`
-   a new transaction (`REQUIRES_NEW`)

Using `AFTER_COMMIT` ensures that asynchronous work is not started for a
transaction that later rolls back.

The executor is bounded through configurable core/max pool sizes and
queue capacity.

------------------------------------------------------------------------

## 💰 Discounts and caching

Discount lookup is implemented through a strategy abstraction.

Active discounts are cached with **Caffeine**:

``` yaml
cache-names: activeDiscounts
expireAfterWrite: 10m
maximumSize: 100
```

This reduces repeated database access for read-heavy reference data.

------------------------------------------------------------------------

## 🧵 Java 21 Virtual Threads

Virtual threads are enabled:

``` yaml
spring:
  threads:
    virtual:
      enabled: true
```

They allow the application to handle blocking request workloads with
lightweight Java threads.

Virtual threads do **not** remove database limits, so the datasource
still uses a bounded HikariCP connection pool.

------------------------------------------------------------------------

## 🌐 REST API

Base URL:

``` text
http://localhost:8080
```

### Authentication

  Method   Endpoint            Description
  -------- ------------------- -----------------------------
  `POST`   `/api/auth/login`   Authenticate and obtain JWT

### Cart

  ----------------------------------------------------------------------------------------------
  Method                  Endpoint                                       Description
  ----------------------- ---------------------------------------------- -----------------------
  `POST`                  `/api/carts`                                   Create a cart for the
                                                                         authenticated user

  `GET`                   `/api/carts`                                   Get authenticated
                                                                         user's carts; optional
                                                                         status filter

  `GET`                   `/api/carts/{cartId}`                          Get one owned cart

  `GET`                   `/api/carts/{cartId}/products`                 List products from an
                                                                         owned cart

  `POST`                  `/api/carts/{cartId}/products`                 Add product / increment
                                                                         quantity

  `PUT`                   `/api/carts/{cartId}/products/{productCode}`   Set product quantity

  `DELETE`                `/api/carts/{cartId}/products/{productCode}`   Remove product

  `POST`                  `/api/carts/{cartId}/process`                  Start asynchronous
                                                                         checkout
  ----------------------------------------------------------------------------------------------

Protected requests use:

``` http
Authorization: Bearer <JWT>
```

------------------------------------------------------------------------

## 📖 Swagger / OpenAPI

Interactive API documentation is available while the application is
running:

``` text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

``` text
http://localhost:8080/v3/api-docs
```

Swagger can be used to inspect the contract and execute API requests.

------------------------------------------------------------------------

## 🗄️ Database

### Development

The default profile is `dev` and uses an in-memory H2 database.

H2 Console:

``` text
http://localhost:8080/h2-console
```

Default development connection:

``` text
JDBC URL: jdbc:h2:mem:cartdb
User:     sa
Password: <empty>
```

The project contains:

-   `schema.sql` --- tables, foreign keys, unique constraints and
    indexes.
-   `data.sql` --- development/test seed data.

The seed data also creates users used by the JMeter stress plans.

> H2 Console is intended for development only. The production profile
> disables it.

### Production profile

Production datasource settings are externalized:

``` text
DB_URL
DB_USERNAME
DB_PASSWORD
DB_DRIVER
JWT_SECRET
```

Optional pool configuration:

``` text
DB_POOL_MAX_SIZE
DB_POOL_MIN_IDLE
DB_CONNECTION_TIMEOUT_MS
```

`JWT_SECRET` has no production fallback value, so production does not
silently start with the development secret.

------------------------------------------------------------------------

## 🐳 Running with Docker Compose

The project contains a multi-stage Dockerfile and a Docker Compose
stack.

Start everything with:

``` bash
docker compose up --build -d
```

Stop the stack with:

``` bash
docker compose down
```

Services:

  Service          Port Purpose
  -------------- ------ ---------------------------------
  `tienda-app`     8080 Spring Boot API
  `prometheus`     9090 Metrics collection
  `grafana`        3000 Dashboards / visualization
  `loki`           3100 Log aggregation
  `promtail`       9080 Log shipping / service endpoint

The Dockerfile uses:

-   Java 21 JDK during build.
-   Java 21 JRE for the runtime image.
-   Maven Wrapper.
-   Dependency caching through Docker layers.

------------------------------------------------------------------------

## 💻 Running locally

Requirements:

-   Java 21
-   Maven Wrapper included in the repository

Linux/macOS:

``` bash
./mvnw spring-boot:run
```

Windows:

``` powershell
mvnw.cmd spring-boot:run
```

The default Spring profile is:

``` text
dev
```

To explicitly select it:

``` bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

------------------------------------------------------------------------

# 📊 Observability

The project includes an observability stack based on:

``` text
Spring Boot Actuator
        │
        ▼
Micrometer
        │
        ▼
Prometheus
        │
        ▼
Grafana
```

and for logs:

``` text
Application / Docker logs
        │
        ▼
Promtail
        │
        ▼
Loki
        │
        ▼
Grafana
```

## Actuator

Exposed endpoints include:

``` text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
```

Health and Prometheus are publicly exposed by the current security
configuration; other Actuator endpoints require an administrator role
when exposed.

Health details use:

``` text
show-details: when_authorized
```

to avoid unnecessarily exposing internal information.

## Prometheus

Prometheus scrapes:

``` text
tienda-app:8080/actuator/prometheus
```

every 5 seconds.

UI:

``` text
http://localhost:9090
```

## Grafana

Grafana:

``` text
http://localhost:3000
```

Docker development credentials:

``` text
Username: admin
Password: admin
```

The repository includes:

``` text
dashboards/spring-boot.json
datasources/prometheus.yml
```

so the project contains a Spring Boot dashboard definition and
Prometheus datasource provisioning.

Useful metrics include:

-   JVM memory.
-   Garbage collection.
-   CPU.
-   HTTP request latency/counts.
-   Thread activity.
-   HikariCP connection pool behavior.
-   Application health.

## Loki + Promtail

The Compose stack also starts Loki and Promtail.

Promtail uses Docker service discovery and forwards container logs to:

``` text
http://loki:3100/loki/api/v1/push
```

The application additionally writes to:

``` text
logs/application.log
```

The current repository automatically provisions Prometheus in Grafana.
If Loki is not already visible in Grafana, add it as a Loki datasource
using:

``` text
http://loki:3100
```

This allows application logs to be queried from Grafana Explore.

------------------------------------------------------------------------

# 🧪 Automated testing

The project contains tests across multiple levels:

-   Domain/entity tests.
-   DTO mapping tests.
-   Controller tests.
-   Service tests.
-   Repository-dependent integration behavior.
-   JWT/security tests.
-   Exception handler tests.
-   Discount strategy tests.
-   Async order processing tests.
-   Concurrency integration tests.

Run all tests:

``` bash
./mvnw test
```

Windows:

``` powershell
mvnw.cmd test
```

The repository also contains generated test/coverage report directories:

``` text
UnitTestReport/
htmlReport/
```

------------------------------------------------------------------------

## 🔀 Concurrency integration test

`CartConcurrencyTest` exercises simultaneous modifications against the
same cart.

The test was increased to **20 concurrent threads**, each adding one
unit of the same product, and validates:

``` text
No concurrency errors
+
20 additions × quantity 1
=
Final quantity 20
```

This test verifies the cart locking strategy at integration level rather
than only mocking repository behavior.

------------------------------------------------------------------------

# 📮 Postman functional tests

A Postman collection is included at:

``` text
docs/Challenge.postman_collection.json
```

The collection covers an end-to-end API workflow including:

1.  Login.
2.  Create cart.
3.  Add cart product.
4.  Remove cart product.
5.  Get cart details.
6.  Get cart products.
7.  Get user carts.
8.  Process cart.

The collection extracts dynamic values such as the authentication token
and cart ID so subsequent requests can reuse them.

To run it:

1.  Start the API.
2.  Open Postman.
3.  Import `docs/Challenge.postman_collection.json`.
4.  Execute the collection or individual requests.

This complements the JUnit suite by validating the application through
its real HTTP API.

------------------------------------------------------------------------

# 🔥 JMeter stress and concurrency testing

The repository contains **three JMeter 5.6.3 test plans** under `docs/`.

These tests are intended to exercise the application through HTTP rather
than through mocked components.

## 1. 100-user stress scenario

``` text
docs/TiendaStressLuchoTest.jmx
```

Configured with:

``` text
100 users
50-second ramp-up
```

Workflow:

``` text
Login
  ↓
Create Cart
  ↓
Add Product
  ↓
Process Order
```

This provides a progressive load scenario across the complete
authenticated checkout flow.

## 2. 1000-user stress scenario

``` text
docs/CartManagementAPI-StressTest.jmx
```

Configured with:

``` text
1000 users
10-second ramp-up
1 iteration per user
```

It exercises:

``` text
POST /api/auth/login
POST /api/carts
POST /api/carts/{cartId}/products
POST /api/carts/{cartId}/process
```

This scenario is useful for observing the application under a sharp
traffic spike while monitoring:

-   HTTP latency and errors.
-   JVM behavior.
-   HikariCP saturation.
-   Thread behavior.
-   Async processing.
-   Prometheus/Grafana metrics.

The development seed script creates **1001 stress-test users** so each
JMeter thread can authenticate independently.

## 3. Same-cart concurrency scenario

``` text
docs/CartManagementAPI-StressTest-Cart.jmx
```

This plan first creates a cart and adds products, then launches:

``` text
5 threads
0-second ramp-up
Synchronizing Timer group size = 5
```

against:

``` text
POST /api/carts/{cartId}/process
```

The Synchronizing Timer releases the five requests together,
intentionally generating contention on the same cart.

This scenario validates the behavior of:

-   cart state validation;
-   pessimistic locking during checkout initiation;
-   idempotency/duplicate-processing defenses;
-   JPA versioning during asynchronous processing;
-   retry/recovery behavior under concurrency.

> The repository contains the executable JMeter plans but does not
> persist a benchmark result file (`.jtl`). Performance numbers
> therefore depend on the machine/environment where the plans are
> executed and are intentionally not hard-coded in this README.

### Running JMeter from CLI

Example:

``` bash
jmeter -n \
  -t docs/CartManagementAPI-StressTest.jmx \
  -l results.jtl \
  -e \
  -o jmeter-report
```

For meaningful performance measurements, run JMeter in non-GUI mode and
observe Grafana/Prometheus at the same time.

------------------------------------------------------------------------

# 🧠 Important engineering decisions

## Why pessimistic locking for cart mutations?

Adding/updating/removing an item is a read-modify-write operation on the
same aggregate.

Serializing mutations of one cart makes the behavior deterministic and
prevents multiple transactions from making decisions from the same stale
collection state.

## Why not pessimistically lock product stock?

Stock is shared by many carts.

Instead of serializing all access to a product, stock deduction uses one
atomic conditional database update. This keeps the invariant
(`stock >= 0`) close to the database and reduces the race window.

## Why both a lock and a unique constraint?

Application locking prevents the normal race.

The database constraint:

``` text
UNIQUE(cart_id, product_id)
```

remains the final integrity boundary.

## Why `AFTER_COMMIT` for checkout events?

Without it, background processing could start before the transaction
that moved the cart to `PROCESSING` was committed.

`AFTER_COMMIT` ensures asynchronous work only begins after the
initiating transaction succeeds.

## Why preserve unit price in `CartItem`?

The cart item stores its unit price instead of always recalculating from
the current product price.

That prevents an existing cart/order line from changing retroactively if
the product catalog price changes later.

## Why scope repository queries by user?

Authorization is enforced as close to the data query as possible:

``` text
cartId + userId
```

This reduces the chance of accidentally exposing another user's cart
through a future service/controller method.

------------------------------------------------------------------------

# 📁 Relevant project files

``` text
.
├── Dockerfile
├── docker-compose.yml
├── prometheus.yml
├── promtail-config.yml
├── dashboards/
│   └── spring-boot.json
├── datasources/
│   └── prometheus.yml
├── docs/
│   ├── Challenge.postman_collection.json
│   ├── CartManagementAPI-StressTest.jmx
│   ├── CartManagementAPI-StressTest-Cart.jmx
│   ├── TiendaStressLuchoTest.jmx
│   └── DER.png
├── src/
│   ├── main/
│   └── test/
├── UnitTestReport/
└── htmlReport/
```

------------------------------------------------------------------------

# 🚦 HTTP error handling

The API centralizes error handling with `GlobalExceptionHandler`.

The application distinguishes between:

-   request/validation errors;
-   unauthorized access;
-   forbidden operations;
-   resources not found;
-   business rule violations;
-   stock conflicts;
-   concurrency conflicts;
-   unexpected server errors.

This keeps controllers focused on HTTP orchestration while error
responses remain consistent.

------------------------------------------------------------------------

# 🧩 Development vs production configuration

Configuration is separated by profile:

``` text
application.yaml
application-dev.yaml
application-prod.yaml
```

### Development

-   H2 in-memory database.
-   H2 console enabled.
-   Seed data enabled.
-   Development JWT secret fallback.

### Production

-   External datasource.
-   H2 console disabled.
-   SQL seed initialization disabled.
-   `open-in-view=false`.
-   JWT secret required through environment.
-   Connection pool configurable through environment variables.

Run production profile with:

``` bash
SPRING_PROFILES_ACTIVE=prod
```

and configure the required environment variables before startup.

------------------------------------------------------------------------

# 🔮 Possible future improvements

The current implementation intentionally keeps infrastructure relatively
lightweight. Possible next steps include:

-   PostgreSQL as the primary production database.
-   Flyway or Liquibase database migrations.
-   Transactional Outbox for guaranteed event publication.
-   RabbitMQ/Kafka for distributed asynchronous processing.
-   Testcontainers for database/infrastructure integration tests.
-   Rate limiting.
-   Distributed cache if the application becomes multi-instance and
    cache consistency requires it.
-   Persisted JMeter benchmark reports for historical performance
    comparison.
-   CI/CD pipeline executing unit, integration and stress smoke tests.
-   Automatic Loki datasource provisioning in Grafana.

------------------------------------------------------------------------

## 👨‍💻 What this project demonstrates

This project goes beyond a basic CRUD API and demonstrates practical
backend concepts including:

-   REST API design.
-   Java 21 / Spring Boot.
-   JWT authentication and authorization.
-   Resource-level ownership.
-   JPA/Hibernate.
-   Pessimistic and optimistic concurrency control.
-   Atomic SQL updates.
-   Database constraints and indexes.
-   Asynchronous processing.
-   Transaction boundaries.
-   Retry/recovery patterns.
-   Caching.
-   Virtual threads.
-   Unit and integration testing.
-   Postman API validation.
-   JMeter stress/concurrency testing.
-   Docker.
-   Metrics and centralized logging.
-   Prometheus, Grafana, Loki and Promtail.
-   Environment-specific configuration.

------------------------------------------------------------------------

## 📄 License

This repository is currently provided as a technical/educational
project. Add the license of your choice if you plan to distribute or
reuse it publicly.
