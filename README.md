# Garland Demo

A working demo for the [Garland](https://github.com/garlandframework/garland) framework.

Two Spring Boot microservices connected through Kafka, with a complete integration test suite written with Garland. The **users** domain has a full reference test suite. The **orders** domain is intentionally left without tests — it exists to be tested using the generation commands.

[![Watch the demo](https://img.youtube.com/vi/bN6O8ek2TjQ/0.jpg)](https://www.youtube.com/watch?v=bN6O8ek2TjQ)

---

## System architecture

```
                        ┌─────────────────────────────┐
                        │        user-service :8080   │
                        │                             │
  HTTP client  ──────►  │  /api/users    PostgreSQL   │
                        │  /api/orders   (userdb)     │
                        │  /api/auth                  │
                        └──────────────┬──────────────┘
                                       │
                               Kafka topics:
                               user.created / user.updated / user.deleted
                               order.placed / order.cancelled
                                       │
                        ┌──────────────▼─────────────────┐
                        │    projection-service :8081    │
                        │                                 │
                        │  /api/projections   MongoDB    │
                        │                  (projectiondb)│
                        └────────────────────────────────┘
```

`user-service` owns users and orders — persists to PostgreSQL, publishes events to Kafka. `projection-service` consumes those events and builds read-model documents in MongoDB.

---

## Prerequisites

- Java 17
- Maven
- Docker + Docker Compose
- Garland framework built locally (`mvn install` in the [framework repo](https://github.com/garlandframework/garland))

---

## Quick start

**1. Start infrastructure and services**

```bash
docker compose up -d --build
```

Wait ~15 seconds for services to be ready.

**2. Run tests**

```bash
mvn test -pl tests
```

All tests run against live services. No mocking.

**Rebuild after code changes:**

```bash
./restart-clean.sh          # full reset — rebuilds images, wipes volumes
./restart-clean.sh --soft   # rebuilds images, keeps data
```

---

## Test suite structure

Tests live in `tests/src/test/java/dev/garlandframework/demo/tests/`.

Four test levels, each covering a different slice of the system:

| Level | Package | What it tests |
|---|---|---|
| **Endpoint** | `users/endpoint/` | Single endpoint — happy path + validation + error cases |
| **Flow** | `users/flow/` | State transitions across a sequence of calls within one service |
| **Component** | `users/component/` | Vertical slice: one entry point through the system boundary it owns |
| **E2E** | `UserEndToEndTest` | Full chain: HTTP → Postgres → Kafka → MongoDB |

Component tests split cleanly by team boundary:
- `UserApiToKafkaTest` — verifies user-service: HTTP in, Kafka event out, DB persisted
- `KafkaToProjectionTest` — verifies projection-service: Kafka event in, MongoDB doc out

---

## Auth

All endpoints require a Bearer JWT. Obtain one via:

```
POST http://localhost:8080/api/auth/login
{ "username": "admin", "password": "admin" }
```

The test suite acquires the token automatically in `@BeforeSuite`. Credentials are in `Connections.java`.

**Swagger UI:**
- user-service: http://localhost:8080/swagger-ui/index.html
- projection-service: http://localhost:8081/swagger-ui/index.html

---

## Orders domain — the blank canvas

`user-service` has a full orders API (`POST /api/orders`, `GET`, `PUT /{id}/cancel`). `projection-service` projects order events to MongoDB. No tests exist for this domain.

This is intentional — it is the domain to practice test generation with Garland.

Use the Claude Code generation commands inside the `tests` module:

```
/gen-endpoint-test PlaceOrderApiTest — full suite
/gen-component-test OrderApiToKafkaTest
/gen-e2e-test OrderEndToEndTest — place and cancel flows
```

The users test suite is the reference for what the generated output should look like.
