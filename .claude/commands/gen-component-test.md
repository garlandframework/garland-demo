# Generate Component Test

Generate a TestNG component test class for this project.
Component tests verify a vertical slice of the system from a specific entry point — without requiring the full stack.
They are distinct from endpoint tests (single endpoint) and end-to-end tests (full system chain).

**Usage:** `/gen-component-test <description of what to generate>`

Examples:
- `/gen-component-test UserApiToKafkaTest — create user publishes event`
- `/gen-component-test KafkaToProjectionTest — event projected to MongoDB`

Read `/Users/volodymyrkobryn/garland/llm.md` first — it contains universal framework rules (pipeline syntax, Verify.allOf, temporal tolerance, anti-patterns).
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
public void createUser_persistedInDb_andPublishesKafkaEvent() {
    Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .then(trackUser())
            .then(Verify.allOf(
                    UserTestMapper.toEntity().andThen(postgresClient.findById()),
                    UserTestMapper.toCreatedEvent().andThen(kafkaClient.consumeMatching(UserCreatedEvent.class))
            ))
            .execute();
}
```

### Slice 2 — `KafkaToProjectionTest` (projection-service team)
Entry point: Kafka. Verifies that projection-service correctly projects a `UserCreatedEvent` into MongoDB.
Does not go through HTTP — uses `TestEvents` to build the event independently.

```java
@Test(description = "A UserCreated Kafka event published directly is projected into MongoDB by the projection-service")
public void userCreatedEvent_projectedToMongo() {
    UserCreatedEvent event = TestUserEvents.defaultUserCreatedEvent();

    Pipeline.given(new KafkaMessage<>(event.userId().toString(), event))
            .then(kafkaClient.publish())   // kafkaClient has user.created as first topic
            .execute();

    UserProjectionDoc expectedDoc = UserTestMapper.INSTANCE.toProjectionDoc(event);
    Pipeline.given(expectedDoc)
            .then(mongoClient.findById())
            .execute();
}
```

Two pipelines: one to publish the event, one to assert the projection. The split reflects two distinct operations.

**MongoDB precision** — `mongoClient.findById()` applies the client's default tolerance automatically (set via `withTemporalTolerance()` in `BaseTest`). Use the explicit `findById(Duration)` overload only when a specific call needs a higher tolerance than the default.

**Counting records by field** — use `countByFields()` + `check.equalTo(NL)` when asserting how many documents/rows match a field pattern. `findByFields()` is strict and throws if more than one result is found — use it only when you expect exactly one match.

```java
UserProjectionDoc template = new UserProjectionDoc(null, "Toyota", null); // only non-null fields used as filter

Pipeline.given(template)
    .then(mongoClient.countByFields())
    .then(check.equalTo(5L))
    .execute();
```

**Important — Kafka client selection:** `publish()` always sends to the first topic registered in the client. For order events (e.g. `order.placed`), use `orderKafkaClient.publish()` — not `kafkaClient.publish()`, which would send to `user.created`.

## Test data factories

```java
// Build a UserCreatedEvent independently of HTTP — no user-service involved
TestUserEvents.defaultUserCreatedEvent()

// Build an OrderPlacedEvent independently
TestOrderEvents.defaultOrderPlacedEvent()
```

`TestUserEvents` / `TestOrderEvents` construct events from scratch using datafaker. Use them whenever the test entry point is Kafka, not HTTP. Do NOT derive events from HTTP responses in `KafkaToProjectionTest` — that would reintroduce a user-service dependency.

## Sequential execution

Component tests share the Kafka topic with other test levels. Run them sequentially (not in parallel) to avoid event contamination between test classes. Configure `thread-count="1"` or `parallel="false"` in `testng.xml`, or annotate the class with `@Test(singleThreaded = true)`.

## Rules

- **Slice 1 stops at Kafka** — do not assert MongoDB in `UserApiToKafkaTest`
- **Slice 2 starts at Kafka** — do not call HTTP in `KafkaToProjectionTest`
- **`TestUserEvents`/`TestOrderEvents` are the only entry points for Slice 2** — never derive events from request factories or HTTP responses
- **Two pipelines in Slice 2** — publish is one pipeline, MongoDB assertion is another
- **No validation/error tests** — those belong in endpoint tests
- **Cross-domain FK in Slice 1 happy-path tests** — `PLACEHOLDER_USER_ID` is valid for validation (400) tests. For happy-path Slice 1 tests that persist an order, create a real user first and use `user.getUuid()`. Services validate FK existence at the database layer, not just at annotation validation.
- **Use the correct Kafka client** — `kafkaClient` for user-domain events, `orderKafkaClient` for order-domain events
- **Track HTTP-created resources** — add `.then(trackUser())` after every `makeCall(201, UserDto.class)` and `.then(trackOrder())` after every `makeCall(201, OrderDto.class)` in Slice 1 tests
- **Class-level description** — put `@Test(description = "...")` on the class as well as on each method. The class description is one sentence summarising the class's scope (e.g. `"Component test: creating a user via HTTP persists it in Postgres and publishes a matching UserCreated event to Kafka"`)

## Imports reference

```java
import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.kafka.model.KafkaMessage;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.users.document.UserProjectionDoc;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.event.UserCreatedEvent;
import org.mtodemo.tests.support.users.factory.TestUserEvents;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.mtodemo.tests.support.users.mapper.UserTestMapper;
import org.mtodemo.tests.support.orders.document.OrderProjectionDoc;
import org.mtodemo.tests.support.orders.event.OrderPlacedEvent;
import org.mtodemo.tests.support.orders.factory.TestOrderEvents;
import org.mtodemo.tests.support.orders.mapper.OrderTestMapper;
import org.testng.annotations.Test;
import java.time.Duration;
```

## Cleanup

Every test that creates a user via HTTP must register it for cleanup by adding `.then(trackUser())` after `makeCall(201, UserDto.class)`. Every test that creates an order must add `.then(trackOrder())` after `makeCall(201, OrderDto.class)`.

`BaseTest` calls the delete/cancel API endpoint for each tracked resource in `@AfterMethod(alwaysRun = true)`, regardless of test pass or fail. Cleanup failures are logged as warnings and never fail the test.

- This applies to Slice 1 (HTTP entry point) — Slice 2 tests that publish events directly via `kafkaClient.publish()` do not create DB-persisted resources through HTTP, so they do not need `trackUser()`
- Never truncate test data directly in the database — it bypasses the application layer and leaves MongoDB projections and Kafka state inconsistent

---

Now generate the component tests described in the argument: **$ARGUMENTS**
