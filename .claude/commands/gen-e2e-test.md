# Generate End-to-End Test

Generate or extend a TestNG end-to-end test class for this project.
E2e tests verify the full cross-system chain: HTTP → Postgres → Kafka → MongoDB.
They are distinct from component tests (single vertical slice) and flow tests (API-level sequence only).

**Usage:** `/gen-e2e-test <description of what to generate>`

Examples:
- `/gen-e2e-test UserEndToEndTest — create full system flow`
- `/gen-e2e-test UserEndToEndTest — update propagates to MongoDB`
- `/gen-e2e-test UserEndToEndTest — delete removes from all systems`

Read `/Users/volodymyrkobryn/ModularTestOrchestrator/ModularTestOrchestrator/untitled/llm.md` first — it contains universal framework rules (pipeline syntax, Verify.allOf, temporal tolerance, anti-patterns).
The rules below are specific to this project.

---

## Project layout

```
tests/src/test/java/org/mtodemo/tests/
  users/
    endpoint/            ← single endpoint tests (/gen-endpoint-test)
    flow/                ← multi-step sequences within user-service (/gen-flow-test)
    component/           ← vertical slice tests (/gen-component-test)
    UserEndToEndTest.java  ← full cross-system chain (this skill)
```

- E2e test classes live directly in `org.mtodemo.tests.users` (not a subdirectory)
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
| `dbClient` | `DbTestClient` | Postgres via Hibernate |
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
            .then(Verify.allOf(
                    UserTestMapper.toEntity().andThen(dbClient.findById()),
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
            .execute();

    UserDto updatePayload = TestUsers.defaultUser();
    Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
            .then(httpClient.makeCall(200, UserDto.class))
            .then(Verify.allOf(
                    UserTestMapper.toEntity().andThen(dbClient.findById()),
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
            .execute();

    Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
            .then(httpClient.makeCall(204, Void.class))
            .execute();

    Pipeline.given(created)
            .then(Verify.allOf(
                    UserTestMapper.toEntity().andThen(dbClient.notExistsById()),
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

Always use the static bridges on the mapper interface (not `StepFunction.lift(INSTANCE::toEntity)`) — the mapper has overloaded methods and type inference will fail for overloaded references.

## DB and MongoDB assertion steps

```java
dbClient.findById()                           // asserts record exists and matches — throws if absent
dbClient.findById(Duration temporalTolerance) // same with timestamp tolerance (Postgres precision)
dbClient.findByFields()                       // asserts unique match — throws if 0 or >1 results
dbClient.countByFields()                      // returns Long count of matching records
dbClient.notExistsById()                      // asserts record is absent — throws if present

mongoClient.findById()                            // asserts document exists and matches — throws if absent
mongoClient.findById(Duration temporalTolerance)  // same with timestamp tolerance — use for any doc with timestamp fields
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

Always use `mongoClient.findById(Duration.ofMillis(1))` when the expected document contains a timestamp field — MongoDB truncates nanoseconds to milliseconds and exact comparison will fail.

## Temporal tolerance

Two situations require temporal tolerance:

**MongoDB precision truncation** — MongoDB stores `Instant` with millisecond precision. Any expected document with a timestamp field must use `findById(Duration.ofMillis(1))`.

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
TestUserRequests.deleteUser(UUID id)
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
- **Cross-domain FK — always create the dependency first** — `PLACEHOLDER_USER_ID` must not be used in e2e tests. E2e tests persist to the database; services validate FK existence at the service/DB layer. Create the referenced entity in a setup pipeline and use the returned UUID.
- **Use the correct Kafka client per domain** — `kafkaClient` for `user.*` events, `orderKafkaClient` for `order.*` events

## Imports reference

```java
import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.event.UserCreatedEvent;
import org.mtodemo.tests.event.UserDeletedEvent;
import org.mtodemo.tests.event.UserUpdatedEvent;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.factory.TestUsers;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.UserTestMapper;
import org.testng.annotations.Test;
import java.time.Duration;
import java.time.Instant;
```

---

Now generate the e2e tests described in the argument: **$ARGUMENTS**
