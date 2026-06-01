# Generate Endpoint Test

Generate or extend a TestNG endpoint test class for this project.

**Usage:** `/gen-endpoint-test <description of what to generate>`

Examples:
- `/gen-endpoint-test CreateUserApiTest — null checks for required fields`
- `/gen-endpoint-test DeleteUserApiTest — full suite`
- `/gen-endpoint-test GetAllUsersApiTest — pagination happy path`

Read `/Users/volodymyrkobryn/ModularTestOrchestrator/ModularTestOrchestrator/untitled/llm.md` first — it contains universal framework rules (pipeline syntax, Verify.allOf, temporal tolerance, anti-patterns).
The rules below are specific to this project — real users replace these with their own factories, mappers, and constraints.

---

## Project layout

```
tests/src/test/java/org/mtodemo/tests/
  users/
    endpoint/   ← single endpoint tests (this skill)
    flow/       ← multi-step sequence tests (/gen-flow-test)
    component/  ← vertical slice tests (/gen-component-test)
    UserEndToEndTest.java  ← full cross-system chain
```

- Endpoint tests live in `org.mtodemo.tests.users.endpoint`
- All test classes extend `BaseTest` (`org.mtodemo.tests.infrastructure.BaseTest`)
- `@Listeners(TestLogger.class)` is inherited from `BaseTest` — do NOT add it again

## Clients (from BaseTest)

| Field | Type | Purpose |
|---|---|---|
| `httpClient` | `HttpTestClient` | HTTP calls to user-service and projection-service |
| `dbClient` | `DbTestClient` | Postgres via Hibernate |
| `kafkaClient` | `KafkaTestClient` | User-domain Kafka (`user.created`, …) |
| `orderKafkaClient` | `KafkaTestClient` | Order-domain Kafka (`order.placed`, …) |
| `mongoClient` | `MongoTestClient` | MongoDB projections |

## Cross-domain FK in endpoint tests

When a domain references another domain's entity (e.g. `OrderRequest.userId`), the factory provides a `PLACEHOLDER_USER_ID`. Use the placeholder **only for validation (400) tests** — the service rejects invalid field values before hitting the DB.

For **happy-path tests** (201/200 expected) that persist data, the referenced entity must exist. Create it first:

```java
UserDto user = Pipeline.given(TestUserRequests.createUser())
        .then(httpClient.makeCall(201, UserDto.class))
        .execute();

Pipeline.given(TestOrderRequests.placeOrder(
                TestOrders.builder().userId(user.getUuid()).build()))
        .then(httpClient.makeCall(201, OrderDto.class))
        ...
```

## Request factories

Always use `TestUserRequests` — never construct `HttpCallRequest` inline in tests:

```java
TestUserRequests.createUser()               // POST /api/users, random valid payload
TestUserRequests.createUser(UserDto dto)    // POST /api/users, specific payload
TestUserRequests.updateUser(UUID id, UserDto dto) // PUT /api/users/{id}
TestUserRequests.getUser(UUID id)           // GET /api/users/{id}
TestUserRequests.getAllUsers()              // GET /api/users
TestUserRequests.deleteUser(UUID id)        // DELETE /api/users/{id}
```

## Test data factories

```java
TestUsers.defaultUser()                    // full valid UserDto (name, surname, address, cars)
TestUsers.builder().name("x").build()      // override specific fields
TestUsers.requiredFieldsOnlyUser()         // name + surname only, no address/cars

TestAddresses.defaultAddress()
TestAddresses.builder().street("x").build()

TestCars.defaultCar()
TestCars.builder().plateNumber("x").build()
```

All builders use `datafaker` for random valid values. Always call `.build()`.

## Mappers (pipeline steps)

```java
UserTestMapper.toEntity()             // UserDto → UserEntity (sets id for DB lookup)
UserTestMapper.toCreatedEvent()       // UserDto → UserCreatedEvent (for Kafka)
UserTestMapper.entityToCreatedEvent() // UserEntity → UserCreatedEvent
UserTestMapper.toProjectionDoc()      // UserDto → UserProjectionDoc (for MongoDB)
```

## DB / Mongo assertion steps

```java
dbClient.findById()                           // asserts record exists and matches — throws if absent
dbClient.findByFields()                       // asserts unique match — throws if 0 or >1 results
dbClient.countByFields()                      // returns Long count of matching records
dbClient.notExistsById()                      // asserts record is absent — throws if present

mongoClient.findById()                            // asserts document exists and matches — throws if absent
mongoClient.findByFields()                        // asserts unique match — throws if 0 or >1 results
mongoClient.countByFields()                       // returns Long count of matching documents
mongoClient.notExistsById()                       // asserts document is absent — throws if present
```

To verify a count of records matching a field pattern:

```java
Car template = new Car(null, null, "Toyota", null); // only non-null fields used as filter

Pipeline.given(template)
    .then(db.countByFields())
    .then(check.equalTo(5L))
    .execute();
```

## Temporal tolerance in HTTP responses

For GET endpoints that return objects with server-generated timestamp fields, use `makeCall(HttpCallResponse<R>, Duration)` instead of `makeCall(int, Class<R>)`. This lets you assert the full response body including the timestamp:

```java
Instant testStart = Instant.now();
UserDto expectedDto = TestUsers.builder().createdAt(testStart).build();  // set timestamp = testStart

Pipeline.given(TestUserRequests.getUser(user.getUuid()))
        .then(httpClient.makeCall(
                new HttpCallResponse<>(200, Map.of(), expectedDto),
                Duration.ofSeconds(5)))
        .execute();
```

For **delayed persistence** (service returns no body, data is written asynchronously) — poll with `pollingCall`:

```java
Pipeline.given(TestUserRequests.getUser(user.getUuid()))
        .then(httpClient.pollingCall(200, expectedDto, RetryConfig.of(10, Duration.ofSeconds(2)), Duration.ofSeconds(5)))
        .execute();
```

For simple status-only or body-only tests where timestamps are irrelevant, continue using `makeCall(int, Class<R>)` and leave timestamp fields `null` in the expected object.

## Error response DTOs

Two DTO shapes depending on the error type:

**`ValidationErrorDto`** — 400 validation errors: `{ status, errors: [{field, message}] }`
```java
// Field name only (message is a Jakarta default — not part of API contract)
Verify.matching(ValidationErrorDto.forField("name"))
Verify.matching(ValidationErrorDto.forField("address.street"))
Verify.matching(ValidationErrorDto.forField("cars[0].plateNumber"))

// Field name + message (only when message is a custom string owned by the API)
Verify.matching(new ValidationErrorDto(400, List.of(new FieldViolationDto("name", "Name is required"))))
```

`forField` sets `status=400` and `message=null`. Null fields are skipped by `Verify.matching`, so only `field` is asserted unless you provide an explicit message.

**`ErrorDto`** — all other errors (404, 409, etc.): `{ status, message }`
```java
// Status only — use when message contains dynamic content (e.g. the resource id)
Verify.matching(ErrorDto.withStatus(404))

// Status + message — use when message is a fixed, meaningful string
Verify.matching(new ErrorDto(404, "User not found"))
```

`withStatus(int)` sets `message=null`, skipping the message assertion.

## Imports reference

```java
import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.modulartestorchestrator.http.model.HttpCallRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import org.mtodemo.tests.dto.ErrorDto;
import org.mtodemo.tests.dto.FieldViolationDto;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.dto.ValidationErrorDto;
import org.mtodemo.tests.factory.TestAddresses;
import org.mtodemo.tests.factory.TestCars;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.factory.TestUsers;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.UserTestMapper;
import org.testng.annotations.Test;
import java.util.List;
import java.util.UUID;
```

## Field constraints for UserRequest

| Field | Constraint | Blank test value | Null test value | Size test value |
|---|---|---|---|---|
| `name` | `@NotBlank`, `@Size(max=100)` | `""` | `null` | `"a".repeat(101)` |
| `surname` | `@NotBlank`, `@Size(max=100)` | `""` | `null` | `"a".repeat(101)` |
| `address.street` | `@NotBlank`, `@Size(max=255)` | `""` | `null` | `"a".repeat(256)` |
| `address.city` | `@NotBlank`, `@Size(max=100)` | `""` | `null` | `"a".repeat(101)` |
| `address.country` | `@NotBlank`, `@Size(max=100)` | `""` | `null` | `"a".repeat(101)` |
| `address.zipCode` | `@NotBlank`, `@Size(max=20)` | `""` | `null` | `"a".repeat(21)` |
| `cars[].plateNumber` | `@NotBlank`, `@Size(max=20)` | `""` | `null` | `"a".repeat(21)` |
| `cars[].manufacturer` | `@NotBlank`, `@Size(max=100)` | `""` | `null` | `"a".repeat(101)` |
| `cars[].model` | `@NotBlank`, `@Size(max=100)` | `""` | `null` | `"a".repeat(101)` |
| `address` | optional | — | — | — |
| `cars` | optional | — | — | — |

For nested fields, always keep the parent object and null/blank only the target field:
```java
UserDto user = TestUsers.builder()
        .address(TestAddresses.builder().street(null).build())
        .build();
```

For car fields, wrap in a list:
```java
UserDto user = TestUsers.builder()
        .cars(List.of(TestCars.builder().plateNumber(null).build()))
        .build();
```

---

Now generate the tests described in the argument: **$ARGUMENTS**
