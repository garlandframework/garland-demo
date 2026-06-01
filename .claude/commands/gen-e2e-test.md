# Generate End-to-End Test

Generate or extend a TestNG end-to-end test class for this project.
E2e tests verify the full cross-system chain: HTTP → Postgres → Kafka → MongoDB.
They are distinct from component tests (single vertical slice) and flow tests (API-level sequence only).

**Usage:** `/gen-e2e-test <description of what to generate>`

Examples:
- `/gen-e2e-test UserEndToEndTest — create full system flow`
- `/gen-e2e-test UserEndToEndTest — update propagates to MongoDB`
- `/gen-e2e-test UserEndToEndTest — delete removes from all systems`

Universal framework rules are in `llm.md` in the MTO repo root.
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

### Create flow — single pipeline walks the full chain

```java
@Test(description = "Creating a user triggers full system flow: Postgres persistence, Kafka event, and MongoDB projection")
public void createUser_fullSystemFlow() throws Exception {
    Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .then(UserTestMapper.toEntity())
            .then(dbClient.findById())
            .then(UserTestMapper.entityToCreatedEvent())
            .then(kafkaClient.consumeMatching(UserCreatedEvent.class))
            .then(UserTestMapper.toProjectionDoc())
            .then(mongoClient.findById())
            .execute();
}
```

The single pipeline works because each step's output is the next step's input in a linear chain.

### Update flow — two pipelines: setup then assert

```java
@Test(description = "Updating a user triggers full system flow: Postgres updated, UserUpdatedEvent published to Kafka, MongoDB projection updated")
public void updateUser_fullSystemFlow() throws Exception {
    UserDto created = Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .execute();

    UserDto updatePayload = TestUsers.defaultUser();
    Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
            .then(httpClient.makeCall(200, UserDto.class))
            .then(UserTestMapper.toEntity())
            .then(dbClient.findById())
            .then(UserTestMapper.entityToUpdatedEvent())
            .then(kafkaClient.consumeMatching(UserUpdatedEvent.class))
            .then(UserTestMapper.toUpdatedProjectionDoc())
            .then(mongoClient.findById())
            .execute();
}
```

Setup (create) is a separate pipeline. Its result is captured to a local variable and reused in the main pipeline.

### Delete flow — four separate pipelines: one per assertion

```java
@Test(description = "Deleting a user triggers full system flow: removed from Postgres, UserDeletedEvent published to Kafka, MongoDB projection removed")
public void deleteUser_fullSystemFlow() throws Exception {
    UserDto created = Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .execute();

    // Pre-compute expected state BEFORE deletion — the entity/doc will no longer be retrievable after
    UserEntity expectedEntity = UserTestMapper.INSTANCE.toEntity(created);
    UserProjectionDoc expectedDoc = UserTestMapper.INSTANCE.toProjectionDoc(
            UserTestMapper.INSTANCE.toCreatedEvent(created));

    Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
            .then(httpClient.makeCall(204, Void.class))
            .execute();

    Pipeline.given(expectedEntity)
            .then(dbClient.notExistsById())
            .execute();

    Pipeline.given(new UserDeletedEvent(created.getUuid(), null))
            .then(kafkaClient.consumeMatching(UserDeletedEvent.class))
            .execute();

    Pipeline.given(expectedDoc)
            .then(mongoClient.notExistsById())
            .execute();
}
```

Four pipelines because the four assertions (HTTP, Postgres, Kafka, MongoDB) are independent — they cannot be chained linearly. Pre-compute the expected entity and doc before the delete, because after deletion the live data is gone.

## Mapper steps (pipeline-compatible)

```java
// UserDto → UserEntity (sets id for DB lookup)
UserTestMapper.toEntity()

// UserDto → UserCreatedEvent
UserTestMapper.toCreatedEvent()

// UserEntity → UserCreatedEvent
UserTestMapper.entityToCreatedEvent()

// UserCreatedEvent → UserProjectionDoc
UserTestMapper.toProjectionDoc()

// UserDto → UserUpdatedEvent
// (used to map HTTP response body to expected event shape)

// UserEntity → UserUpdatedEvent
UserTestMapper.entityToUpdatedEvent()

// UserUpdatedEvent → UserProjectionDoc (overload for update flow)
UserTestMapper.toUpdatedProjectionDoc()

// Direct mapper calls (not pipeline steps):
UserTestMapper.INSTANCE.toEntity(UserDto)
UserTestMapper.INSTANCE.toProjectionDoc(UserCreatedEvent)
UserTestMapper.INSTANCE.toCreatedEvent(UserDto)
```

## DB and MongoDB assertion steps

```java
dbClient.findById()        // asserts record exists and matches — throws if absent
dbClient.notExistsById()   // asserts record is absent — throws if present

mongoClient.findById()     // asserts document exists and matches — throws if absent
mongoClient.notExistsById() // asserts document is absent — throws if present
```

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
- **null fields in expected objects are skipped** by `consumeMatching` — use `null` for fields you cannot predict (e.g. `eventTimestamp` in `UserDeletedEvent`)
- **No validation or error tests** — blank/null/size tests belong in endpoint tests
- **description** reads as a system-level story: "Creating a user triggers full system flow: ...", not "Test create e2e"
- **Cross-domain FK — always create the dependency first** — `PLACEHOLDER_USER_ID` must not be used in e2e tests. E2e tests persist to the database; services validate FK existence at the service/DB layer. Create the referenced entity in a setup pipeline and use the returned UUID.
- **Use the correct Kafka client per domain** — `kafkaClient` for `user.*` events, `orderKafkaClient` for `order.*` events

## Imports reference

```java
import org.modulartestorchestrator.base.Pipeline;
import org.mtodemo.tests.document.UserProjectionDoc;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.entity.UserEntity;
import org.mtodemo.tests.event.UserCreatedEvent;
import org.mtodemo.tests.event.UserDeletedEvent;
import org.mtodemo.tests.event.UserUpdatedEvent;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.factory.TestUsers;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.UserTestMapper;
import org.testng.annotations.Test;
```

---

Now generate the e2e tests described in the argument: **$ARGUMENTS**
