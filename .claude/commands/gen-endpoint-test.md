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
- All test classes extend `BaseTest` (`org.mtodemo.tests.support.base.BaseTest`)
- `@Listeners(TestLogger.class)` is inherited from `BaseTest` — do NOT add it again

## Clients (from BaseTest)

| Field | Type | Purpose |
|---|---|---|
| `httpClient` | `HttpTestClient` | HTTP calls to user-service and projection-service |
| `postgresClient` | `PostgresTestClient` | Postgres via Hibernate |
| `kafkaClient` | `KafkaTestClient` | User-domain Kafka (`user.created`, …) |
| `orderKafkaClient` | `KafkaTestClient` | Order-domain Kafka (`order.placed`, …) |
| `mongoClient` | `MongoTestClient` | MongoDB projections |

## Cross-domain FK in endpoint tests

When a domain references another domain's entity (e.g. `OrderRequest.userId`), the factory provides a `PLACEHOLDER_USER_ID`. Use the placeholder **only for validation (400) tests** — the service rejects invalid field values before hitting the DB.

For **happy-path tests** (201/200 expected) that persist data, the referenced entity must exist. Create it first:

```java
UserDto user = Pipeline.given(TestUserRequests.createUser())
        .then(httpClient.makeCall(201, UserDto.class))
        .then(trackUser())
        .execute();

Pipeline.given(TestOrderRequests.placeOrder(
                TestOrders.builder().userId(user.getUuid()).build()))
        .then(httpClient.makeCall(201, OrderDto.class))
        ...
```

## Auth tests

Negative auth tests live in `AuthTest` (same package). Use `httpClient.withoutHeader` / `withBearer` / `withBaseUrl` inline — never reassign the shared `httpClient`:

```java
// No token → 401
Pipeline.given(TestUserRequests.createUser())
        .then(httpClient.withoutHeader("Authorization")
                .makeCall(new HttpCallResponse<>(401, Map.of(), ErrorDto.withStatus(401))))
        .execute();

// Invalid JWT → 401
Pipeline.given(TestUserRequests.createUser())
        .then(httpClient.withBearer("not-a-valid-jwt")
                .makeCall(new HttpCallResponse<>(401, Map.of(), ErrorDto.withStatus(401))))
        .execute();

// Wrong credentials on login → 401
Pipeline.given(TestAuthRequests.login("admin", "wrong-password"))
        .then(httpClient.makeCall(new HttpCallResponse<>(401, Map.of(), ErrorDto.withStatus(401))))
        .execute();
```

Auth token is acquired once in `@BeforeSuite` and wired into `httpClient` automatically — no per-test setup needed for happy-path tests.

## Request factories

Always use factory methods — never construct `HttpCallRequest` inline in tests. This includes form-encoded and multipart requests.

```java
TestUserRequests.createUser()                    // POST /api/users, random valid payload
TestUserRequests.createUser(UserDto dto)         // POST /api/users, specific payload
TestUserRequests.updateUser(UUID id, UserDto dto) // PUT /api/users/{id}
TestUserRequests.getUser(UUID id)                // GET /api/users/{id}
TestUserRequests.getAllUsers()                   // GET /api/users (page=0, size=20 by default)
TestUserRequests.deleteUser(UUID id)             // DELETE /api/users/{id}
TestUserRequests.exportUser(UUID id)             // GET /api/users/{id}/export → CSV download

TestAuthRequests.login()                         // POST /api/auth/login, admin credentials
TestAuthRequests.login(String user, String pwd)  // POST /api/auth/login, custom credentials
TestAuthRequests.oauthToken()                    // POST /oauth/token, form-encoded client_credentials

TestFileRequests.uploadFromDisk(String desc, Path file, String contentType)   // POST /api/files (file from disk)
TestFileRequests.uploadFromBytes(String desc, byte[] data, String filename, String contentType) // POST /api/files (in-memory)
```

## Content-type bodies

Most endpoints use JSON (`@RequestBody`). Three non-JSON body types are also supported via factory methods.

**FormBody** — `POST /oauth/token` accepts `application/x-www-form-urlencoded`. Use `TestAuthRequests.oauthToken()`.

**MultipartBody** — `POST /api/files` accepts `multipart/form-data`. Use `TestFileRequests.uploadFromDisk` or `TestFileRequests.uploadFromBytes`. The part name `"file"` must match the server's `@RequestParam` name.

**Raw string body** — pass a `String` as the `dto` to skip Jackson and send it as-is. Use inline `HttpCallRequest` only when the raw body itself is what the test is parameterising (fixture replay, adversarial strings). Content-Type defaults to `application/json`; add a header to override:
```java
new HttpCallRequest<>(Connections.USER_SERVICE_URL + "/api/users", "POST", List.of(),
        "{\"name\":\"Alice\",\"surname\":\"Smith\"}")
```

`Content-Type` is set automatically for FormBody and MultipartBody — do not add it manually.

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
postgresClient.findById()                           // asserts record exists and matches — throws if absent
postgresClient.findByFields()                       // asserts unique match — throws if 0 or >1 results
postgresClient.countByFields()                      // returns Long count of matching records
postgresClient.notExistsById()                      // asserts record is absent — throws if present

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
import org.modulartestorchestrator.http.model.HttpCallResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.common.dto.ErrorDto;
import org.mtodemo.tests.support.common.dto.FieldViolationDto;
import org.mtodemo.tests.support.common.dto.LoginRequest;
import org.mtodemo.tests.support.common.dto.TokenDto;
import org.mtodemo.tests.support.common.dto.ValidationErrorDto;
import org.mtodemo.tests.support.common.factory.TestAuthRequests;
import org.mtodemo.tests.support.common.factory.TestFileRequests;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestAddresses;
import org.mtodemo.tests.support.users.factory.TestCars;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.mtodemo.tests.support.users.factory.TestUsers;
import org.mtodemo.tests.support.users.mapper.UserTestMapper;
import org.testng.annotations.Test;
import java.util.List;
import java.util.Map;
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

## Query parameter constraints

`GET /api/users` accepts:

| Param | Type | Constraint | Invalid value | Malformed value |
|-------|------|------------|---------------|-----------------|
| `page` | Integer | `>= 0` | `-1` | `"abc"` |
| `size` | Integer | `1–100` | `0`, `101` | `"xyz"` |

Use `withQueryParam` / `withQueryParams` — never string-concat params into the URL.

**Important:** `@RequestParam` constraint violations (`@Min`/`@Max`) throw `ConstraintViolationException`, which produces `{status:400, message:"..."}`. Deserialise as `ErrorDto`, **not** `ValidationErrorDto`. `ValidationErrorDto` is only for `@RequestBody` validation (`MethodArgumentNotValidException`).

```java
// happy path — single param
Pipeline.given(TestUserRequests.getAllUsers().withQueryParam("page", "0"))
        .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
        .execute();

// happy path — multiple params
Pipeline.given(TestUserRequests.getAllUsers().withQueryParams(Map.of("page", "0", "size", "10")))
        .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
        .execute();

// invalid value — ConstraintViolationException → ErrorDto (not ValidationErrorDto)
Pipeline.given(TestUserRequests.getAllUsers().withQueryParam("page", "-1"))
        .then(httpClient.makeCall(400, ErrorDto.class))
        .execute();

// malformed (cannot be parsed as Integer)
Pipeline.given(TestUserRequests.getAllUsers().withQueryParam("page", "abc"))
        .then(httpClient.makeCall(400, ErrorDto.class))
        .execute();
```

## @BeforeMethod for update/delete test classes

When every test in a class requires a pre-existing entity (e.g. a PUT or DELETE endpoint test), declare the entity as an instance field and create it once per test in `@BeforeMethod`. Do not repeat the setup pipeline inside each test method.

```java
public class UpdateUserApiTest extends BaseTest {

    private UserDto created;

    @BeforeMethod
    public void createUser() {
        created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    @Test(description = "...")
    public void updateUser_blankName_returns400() {
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), TestUsers.builder().name("").build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("name")))
                .execute();
    }
}
```

`trackUser()` is called in `@BeforeMethod` — `@AfterMethod(alwaysRun = true)` in `BaseTest` cleans up the registered resource after every test, including on failure.

Use this pattern whenever a class has more than two tests that share an identical setup pipeline. If only one or two tests need setup, keep it inline.

Import needed: `import org.testng.annotations.BeforeMethod;`

## Cleanup

Every test that creates a user must register it for cleanup by adding `.then(trackUser())` immediately after `makeCall(201, UserDto.class)`. Every test that creates an order must add `.then(trackOrder())` after `makeCall(201, OrderDto.class)`.

```java
UserDto user = Pipeline.given(TestUserRequests.createUser())
        .then(httpClient.makeCall(201, UserDto.class))
        .then(trackUser())
        .execute();
```

`BaseTest` calls the delete/cancel API endpoint for each tracked resource in `@AfterMethod(alwaysRun = true)`, regardless of test pass or fail. Cleanup failures are logged as warnings and never fail the test.

- Always call `trackUser()`/`trackOrder()` even if the test itself deletes or cancels the resource — if the test fails before its own cleanup step, the tracker ensures it still runs
- Never truncate test data directly in the database — it bypasses the application layer and leaves MongoDB projections and Kafka state inconsistent

---

Now generate the tests described in the argument: **$ARGUMENTS**
