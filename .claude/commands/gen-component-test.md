# Generate Component Test

Generate a TestNG component test class for this project.
Component tests verify a vertical slice of the system from a specific entry point — without requiring the full stack.
They are distinct from endpoint tests (single endpoint) and end-to-end tests (full system chain).

**Usage:** `/gen-component-test <description of what to generate>`

Examples:
- `/gen-component-test UserApiToKafkaTest — create user publishes event`
- `/gen-component-test KafkaToProjectionTest — event projected to MongoDB`

Universal framework rules are in `llm.md` in the MTO repo root.
The rules below are specific to this project.

---

## Project layout

```
tests/src/test/java/org/mtodemo/tests/
  users/
    endpoint/      ← single endpoint tests (/gen-endpoint-test)
    flow/          ← multi-step sequences within user-service (/gen-flow-test)
    component/     ← vertical slice tests (this skill)
    UserEndToEndTest.java  ← full cross-system chain
```

## Package

`org.mtodemo.tests.users.component`

## Two component slices

### Slice 1 — `UserApiToKafkaTest` (user-service team)
Entry point: HTTP. Verifies that user-service correctly persists to Postgres AND publishes the right Kafka event.
Does not assert MongoDB — that is projection-service's responsibility.

```java
@Test(description = "Creating a user via HTTP persists it in Postgres and publishes a matching UserCreated event to Kafka")
public void createUser_persistedInDb_andPublishesKafkaEvent() throws Exception {
    Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .then(UserTestMapper.toEntity())
            .then(dbClient.findById())
            .then(UserTestMapper.entityToCreatedEvent())
            .then(kafkaClient.consumeMatching(UserCreatedEvent.class))
            .execute();
}
```

### Slice 2 — `KafkaToProjectionTest` (projection-service team)
Entry point: Kafka. Verifies that projection-service correctly projects a `UserCreatedEvent` into MongoDB.
Does not go through HTTP — uses `TestEvents` to build the event independently.

```java
@Test(description = "A UserCreated Kafka event published directly is projected into MongoDB by the projection-service")
public void userCreatedEvent_projectedToMongo() throws Exception {
    UserCreatedEvent event = TestEvents.defaultUserCreatedEvent();

    Pipeline.given(new KafkaMessage<>(event.userId().toString(), event))
            .then(kafkaClient.publish())   // kafkaClient has user.created as first topic
            .execute();

    UserProjectionDoc expectedDoc = UserTestMapper.INSTANCE.toProjectionDoc(event);
    Pipeline.given(expectedDoc)
            .then(mongoClient.findById(Duration.ofMillis(1)))
            .execute();
}
```

Two pipelines: one to publish the event, one to assert the projection. The split reflects two distinct operations.

**MongoDB precision** — always use `mongoClient.findById(Duration.ofMillis(1))` when the expected document contains a timestamp field. MongoDB truncates `Instant` nanoseconds to milliseconds; exact comparison will fail without tolerance.

**Important — Kafka client selection:** `publish()` always sends to the first topic registered in the client. For order events (e.g. `order.placed`), use `orderKafkaClient.publish()` — not `kafkaClient.publish()`, which would send to `user.created`.

## Test data factories

```java
// Build a UserCreatedEvent independently of HTTP — no user-service involved
TestEvents.defaultUserCreatedEvent()
```

`TestEvents` constructs events from scratch using datafaker. Use it whenever the test entry point is Kafka, not HTTP. Do NOT derive events from HTTP responses in `KafkaToProjectionTest` — that would reintroduce a user-service dependency.

## Sequential execution

Component tests share the Kafka topic with other test levels. Run them sequentially (not in parallel) to avoid event contamination between test classes. Configure `thread-count="1"` or `parallel="false"` in `testng.xml`, or annotate the class with `@Test(singleThreaded = true)`.

## Rules

- **Slice 1 stops at Kafka** — do not assert MongoDB in `UserApiToKafkaTest`
- **Slice 2 starts at Kafka** — do not call HTTP in `KafkaToProjectionTest`
- **`TestEvents` is the only entry point for Slice 2** — never derive events from `TestUserRequests` or HTTP responses
- **Two pipelines in Slice 2** — publish is one pipeline, MongoDB assertion is another
- **No validation/error tests** — those belong in endpoint tests
- **Cross-domain FK in Slice 1 happy-path tests** — `PLACEHOLDER_USER_ID` is valid for validation (400) tests. For happy-path Slice 1 tests that persist an order, create a real user first and use `user.getUuid()`. Services validate FK existence at the database layer, not just at annotation validation.
- **Use the correct Kafka client** — `kafkaClient` for user-domain events, `orderKafkaClient` for order-domain events

## Imports reference

```java
import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.kafka.model.KafkaMessage;
import org.mtodemo.tests.document.UserProjectionDoc;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.event.UserCreatedEvent;
import org.mtodemo.tests.factory.TestEvents;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.UserTestMapper;
import org.testng.annotations.Test;
import java.time.Duration;
```

---

Now generate the component tests described in the argument: **$ARGUMENTS**
