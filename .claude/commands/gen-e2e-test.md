# Generate End-to-End Test

Generate or extend a TestNG end-to-end test class for this project.
E2e tests verify the full cross-system chain: HTTP → Postgres → Kafka → MongoDB.
They are distinct from component tests (single vertical slice) and flow tests (API-level sequence only).

**Usage:** `/gen-e2e-test <description of what to generate>`

Examples:
- `/gen-e2e-test UserEndToEndTest — create full system flow`
- `/gen-e2e-test UserEndToEndTest — update propagates to MongoDB`
- `/gen-e2e-test UserEndToEndTest — delete removes from all systems`

Read `/Users/volodymyrkobryn/garland/llm.md` first — it contains universal framework rules (pipeline syntax, Verify.allOf, temporal tolerance, anti-patterns).
The rules below are specific to this project.

---

## Project layout

```
tests/src/test/java/dev/garlandframework/demo/tests/
  users/
    endpoint/            ← single endpoint tests (/gen-endpoint-test)
    flow/                ← multi-step sequences within user-service (/gen-flow-test)
    component/           ← vertical slice tests (/gen-component-test)
    UserEndToEndTest.java  ← full cross-system chain (this skill)
```

- E2e test classes live directly in `dev.garlandframework.demo.tests.users` (not a subdirectory)
- Class name convention: `<Domain>EndToEndTest`
- All test classes extend `BaseTest`

## System topology

```
HTTP request
    └─► user-service ──► Postgres (via JPA)
                     └─► Kafka (event published)
                              └─► projection-service ──► MongoDB
```

A full e2e test walks this entire chain in order. Each system is asserted explicitly.

## Clients (from BaseTest)

| Field | Type | Purpose |
|---|---|---|
| `httpClient` | `HttpTestClient` | HTTP calls to user-service |
| `postgresClient` | `PostgresTestClient` | Postgres via Hibernate |
| `kafkaClient` | `KafkaTestClient` | User-domain Kafka — `user.created`, `user.updated`, `user.deleted` |
| `orderKafkaClient` | `KafkaTestClient` | Order-domain Kafka — `order.placed`, `order.cancelled` |
| `mongoClient` | `MongoTestClient` | MongoDB projections |

`publish()` and `consumeMatching()` operate on the first registered topic in a client. Use `kafkaClient` for user events and `orderKafkaClient` for order events.

## Pipeline structure

**One pipeline per logical operation.** State (UUID, DTO, entity) is carried between pipelines via local variables — not through context.

### Create / update flow — fan out with Verify.allOf()

After the HTTP call, use `Verify.allOf()` to fan out to all independent side-effect assertions in one step. All branches receive the same `UserDto` result. All failures are reported together.

```java
@Test(description = "Creating a user triggers full system flow: Postgres persistence, Kafka event, and MongoDB projection")
public void createUser_fullSystemFlow() {
    Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .then(trackUser())
            .then(Verify.allOf(
                    UserTestMapper.toEntity().andThen(postgresClient.findById()),
                    UserTestMapper.toCreatedEvent().andThen(kafkaClient.consumeMatching(UserCreatedEvent.class)),
                    UserTestMapper.dtoToCreatedProjectionDoc().andThen(mongoClient.findById())
            ))
            .execute();
}
```

```java
@Test(description = "Updating a user triggers full system flow: Postgres updated, UserUpdatedEvent published to Kafka, MongoDB projection updated")
public void updateUser_fullSystemFlow() {
    UserDto created = Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .then(trackUser())
            .execute();

    UserDto updatePayload = TestUsers.defaultUser();
    Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
            .then(httpClient.makeCall(200, UserDto.class))
            .then(Verify.allOf(
                    UserTestMapper.toEntity().andThen(postgresClient.findById()),
                    UserTestMapper.toUpdatedEvent().andThen(kafkaClient.consumeMatching(UserUpdatedEvent.class)),
                    UserTestMapper.dtoToUpdatedProjectionDoc().andThen(mongoClient.findById())
            ))
            .execute();
}
```

### Delete flow — delete call, then allOf on captured DTO

The delete returns `Void`, so start a new pipeline from the pre-delete `created` DTO and fan out to verify absence in all systems.

```java
@Test(description = "Deleting a user triggers full system flow: removed from Postgres, UserDeletedEvent published to Kafka, MongoDB projection removed")
public void deleteUser_fullSystemFlow() {
    UserDto created = Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .then(trackUser())
            .execute();

    Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
            .then(httpClient.makeCall(204, Void.class))
            .execute();

    Pipeline.given(created)
            .then(Verify.allOf(
                    UserTestMapper.toEntity().andThen(postgresClient.notExistsById()),
                    UserTestMapper.toDeletedEvent().andThen(kafkaClient.consumeMatching(UserDeletedEvent.class)),
                    UserTestMapper.dtoToCreatedProjectionDoc().andThen(mongoClient.notExistsById())
            ))
            .execute();
}
```

## Mapper steps (pipeline-compatible)

```java
// UserDto → UserEntity (sets id for DB lookup)
UserTestMapper.toEntity()

// UserDto → UserCreatedEvent
UserTestMapper.toCreatedEvent()

// UserDto → UserUpdatedEvent
UserTestMapper.toUpdatedEvent()

// UserDto → UserDeletedEvent
UserTestMapper.toDeletedEvent()

// UserDto → UserProjectionDoc (via toCreatedEvent internally)
UserTestMapper.dtoToCreatedProjectionDoc()

// UserDto → UserProjectionDoc (via toUpdatedEvent internally)
UserTestMapper.dtoToUpdatedProjectionDoc()

// UserEntity → UserCreatedEvent
UserTestMapper.entityToCreatedEvent()

// UserCreatedEvent → UserProjectionDoc
UserTestMapper.toProjectionDoc()

// UserUpdatedEvent → UserProjectionDoc
UserTestMapper.toUpdatedProjectionDoc()
```

Always use the static bridges on the mapper interface (not `Step.lift(INSTANCE::toEntity)`) — the mapper has overloaded methods and type inference will fail for overloaded references.

## DB and MongoDB assertion steps

```java
postgresClient.findById()                           // asserts record exists and matches — throws if absent
postgresClient.findById(Duration temporalTolerance) // override default tolerance for this call
postgresClient.findByFields()                       // asserts unique match — throws if 0 or >1 results
postgresClient.countByFields()                      // returns Long count of matching records
postgresClient.notExistsById()                      // asserts record is absent — throws if present

mongoClient.findById()                            // asserts document exists and matches — throws if absent
mongoClient.findById(Duration temporalTolerance)  // override default tolerance for this call
mongoClient.findByFields()                        // asserts unique match — throws if 0 or >1 results
mongoClient.countByFields()                       // returns Long count of matching documents
mongoClient.notExistsById()                       // asserts document is absent — throws if present
```

To verify a count of records matching a field pattern:

```java
Car template = new Car(null, null, "Toyota", null); // only non-null fields are used as filter

Pipeline.given(template)
    .then(db.countByFields())
    .then(check.equalTo(5L))
    .execute();
```

`mongoClient.findById()` applies the client's default tolerance automatically (set via `withTemporalTolerance()` in `BaseTest`). Use `findById(Duration)` only when overriding for a specific call that needs a higher tolerance.

## Temporal tolerance

Two situations require temporal tolerance:

**Storage precision truncation** — MongoDB truncates `Instant` to milliseconds; PostgreSQL to microseconds. The client-level defaults set via `withTemporalTolerance()` in `BaseTest` handle this automatically — use bare `findById()` and `consumeMatching()` in tests.

**Service-generated timestamps** — When the service sets a timestamp internally (e.g. `eventTimestamp = Instant.now()`), capture the test start time and use it as the expected value with a tolerance equal to the maximum acceptable processing delay:

```java
Instant testStart = Instant.now();

OrderPlacedEvent expected = new OrderPlacedEvent(orderId, ..., testStart);
Pipeline.given(expected)
        .then(orderKafkaClient.consumeMatching(OrderPlacedEvent.class, Duration.ofMinutes(2)))
        .execute();
```

This asserts the timestamp is present and within the SLA window — if processing takes more than 2 minutes, the test fails.

## Request factories

```java
TestUserRequests.createUser()
TestUserRequests.createUser(UserDto dto)
TestUserRequests.updateUser(UUID id, UserDto dto)
TestUserRequests.getUser(UUID id)
TestUserRequests.getAllUsers()
TestUserRequests.deleteUser(UUID id)
```

Query parameters (when an endpoint accepts them) are added via `withQueryParam` / `withQueryParams` — never string-concat into the URL:

```java
TestUserRequests.getAllUsers().withQueryParam("page", "0")
TestUserRequests.getAllUsers().withQueryParams(Map.of("page", "0", "size", "10"))
```

## Test data factories

```java
TestUsers.defaultUser()                    // full valid UserDto
TestUsers.builder().name("x").build()      // override specific fields
TestUsers.requiredFieldsOnlyUser()         // name + surname only
```

## Rules

- **Assert all four systems** — an e2e test that skips Kafka or MongoDB is a component test, not an e2e test
- **One pipeline per logical operation** — do not chain create→delete in one pipeline
- **Pre-compute expected state before a destructive operation** — capture entity/doc before calling delete so `notExistsById` has something to check
- **Prefer temporal tolerance over `null` for timestamp fields** — `null` skips the field entirely; tolerance still verifies the field is present and within bounds. Use `consumeMatching(Class, Duration)` for events with service-generated timestamps. Use `null` only for truly unpredictable non-temporal fields.
- **No validation or error tests** — blank/null/size tests belong in endpoint tests
- **description** reads as a system-level story: "Creating a user triggers full system flow: ...", not "Test create e2e"
- **Class-level description** — put `@Test(description = "...")` on the class as well as on each method. The class description is one sentence summarising the class's scope (e.g. `"End-to-end tests for user flows: create, update, delete each trigger the full system flow across Postgres, Kafka, and MongoDB"`)
- **Cross-domain FK — always create the dependency first** — `PLACEHOLDER_USER_ID` must not be used in e2e tests. E2e tests persist to the database; services validate FK existence at the service/DB layer. Create the referenced entity in a setup pipeline and use the returned UUID.
- **Use the correct Kafka client per domain** — `kafkaClient` for `user.*` events, `orderKafkaClient` for `order.*` events
- **Track every created resource** — call `.then(trackUser())` after every `makeCall(201, UserDto.class)` and `.then(trackOrder())` after every `makeCall(201, OrderDto.class)`, even if the test itself performs the deletion

## Imports reference

```java
import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.event.UserCreatedEvent;
import dev.garlandframework.demo.tests.support.users.event.UserDeletedEvent;
import dev.garlandframework.demo.tests.support.users.event.UserUpdatedEvent;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
import dev.garlandframework.demo.tests.support.users.factory.TestUsers;
import dev.garlandframework.demo.tests.support.users.mapper.UserTestMapper;
import dev.garlandframework.demo.tests.support.orders.dto.OrderDto;
import dev.garlandframework.demo.tests.support.orders.event.OrderPlacedEvent;
import dev.garlandframework.demo.tests.support.orders.event.OrderCancelledEvent;
import dev.garlandframework.demo.tests.support.orders.factory.TestOrderRequests;
import dev.garlandframework.demo.tests.support.orders.factory.TestOrders;
import dev.garlandframework.demo.tests.support.orders.mapper.OrderTestMapper;
import org.testng.annotations.Test;
import java.time.Duration;
import java.time.Instant;
```

## Cleanup

Every test that creates a user must add `.then(trackUser())` after `makeCall(201, UserDto.class)`. Every test that creates an order must add `.then(trackOrder())` after `makeCall(201, OrderDto.class)`.

`BaseTest` calls the delete/cancel API endpoint for each tracked resource in `@AfterMethod(alwaysRun = true)`, regardless of test pass or fail. Cleanup failures are logged as warnings and never fail the test.

- Always track even when the test itself performs the deletion — if the test fails mid-flow, the tracker is the only safety net
- Never truncate test data directly in the database — it bypasses the application layer and leaves Kafka events and MongoDB projections in an inconsistent state

---

Now generate the e2e tests described in the argument: **$ARGUMENTS**
