# CLAUDE.md

This is the demo project for the Modular Test Orchestrator (MTO) framework. Two Spring Boot microservices (`user-service` on :8080, `projection-service` on :8081) connected through Kafka, tested end-to-end without mocking.

The **users** domain has a complete reference test suite. The **orders** domain is intentionally untested — it exists to be covered using the generation commands.

---

## Build & run

```bash
# Start infrastructure and services (first run or after code changes)
docker compose up -d --build

# Rebuild and restart — wipes data
./restart-clean.sh

# Rebuild and restart — keeps data
./restart-clean.sh --soft

# Run all tests (services must be running)
mvn test -pl tests

# Run a single test class
mvn test -pl tests -Dtest=CreateUserApiTest

# Build services only (no Docker)
mvn clean package -DskipTests -pl user-service
mvn clean package -DskipTests -pl projection-service
```

Services depend on the MTO framework being installed locally. Build the framework first if needed: `mvn install` in the framework repo.

---

## Project structure

```
user-service/        Spring Boot :8080 — users + orders API, PostgreSQL, Kafka producer
projection-service/  Spring Boot :8081 — projection read model, MongoDB, Kafka consumer
tests/               Integration test module — all tests live here
docker-compose.yml   PostgreSQL, MongoDB, Kafka, both services
restart-clean.sh     Rebuild + restart helper
```

---

## Test module layout

```
tests/src/test/java/org/mtodemo/tests/
  infrastructure/     BaseTest, Connections, TestLogger
  dto/                HTTP-layer DTOs (UserDto, OrderDto, ErrorDto, LoginRequest, …)
  entity/             Hibernate entities for Postgres assertions
  document/           MongoDB document classes for projection assertions
  event/              Kafka event record types
  factory/            Test data builders (TestUsers, TestOrders, TestUserRequests, …)
  mapper/             MapStruct mapper bridges (UserTestMapper, OrderTestMapper)
  users/              Reference test suite (complete)
    endpoint/         Single-endpoint tests — happy path + validation
    flow/             Multi-step sequences within user-service
    component/        UserApiToKafkaTest, KafkaToProjectionTest
    UserEndToEndTest  Full HTTP → Postgres → Kafka → MongoDB chain
  orders/             Blank canvas — no tests, domain exists in services
```

Key infrastructure files:
- `Connections.java` — all host/port/credential constants
- `BaseTest.java` — `@BeforeSuite` wires up all clients (HTTP, DB, Kafka×2, Mongo) and acquires JWT
- `TestLogger.java` — TestNG listener, logs per-test pass/fail

---

## Generation commands

All generation commands live in `.claude/commands/`. Use them inside the `tests` module. They read universal framework rules from `llm.md` in the framework repo automatically.

| Command | Generates |
|---|---|
| `/gen-endpoint-test <description>` | Single-endpoint tests: happy path + all validation cases |
| `/gen-flow-test <description>` | State-transition sequence tests within one service |
| `/gen-component-test <description>` | Vertical slice from one entry point (HTTP→Kafka or Kafka→Mongo) |
| `/gen-e2e-test <description>` | Full cross-system chain test |
| `/coverage-report` | Gap analysis: which endpoints and paths have no tests |
| `/setup` | Bootstraps test infrastructure for a new project |
| `/gen-project-commands` | Generates project-specific gen commands from source scan |

The users test suite is the reference for what generated output should look like.

---

## Auth

All service endpoints require a Bearer JWT. Credentials: `admin` / `admin`.

`BaseTest.setUpSuite()` acquires the token via Pipeline and wires it into `httpClient` automatically — tests do not manage auth.

For negative auth tests, use `httpClient.withoutHeader("Authorization")` or `httpClient.withBearer("wrong-token")` inline. Never reassign the shared `httpClient`.

---

## Conventions

- **Factories** return `HttpCallRequest<T>` (request factories) or builder-based DTOs (data factories). Never construct `HttpCallRequest` inline in tests.
- **Mappers** use static bridge methods (`UserTestMapper.toEntity()`, not `INSTANCE::toEntity`) — overloaded methods break type inference.
- **allOf** for fan-out: when one HTTP response triggers DB + Kafka + Mongo assertions, use `Verify.allOf()` not sequential chains.
- **Temporal tolerance**: MongoDB truncates `Instant` to milliseconds — always use `mongoClient.findById(Duration.ofMillis(1))` when the document contains a timestamp.
- Test DTOs duplicate service classes intentionally — tests must not depend on service internals.
