# Generate Flow Test

Generate a TestNG flow test class for this project.
Flow tests verify state transitions and consistency across a sequence of endpoint calls within a single service.
They are distinct from endpoint tests (single endpoint, happy + negative) and e2e tests (cross-system).

**Usage:** `/gen-flow-test <description of what to generate>`

Examples:
- `/gen-flow-test UserFlowTest — create then get`
- `/gen-flow-test UserFlowTest — full CRUD sequence`
- `/gen-flow-test UserFlowTest — delete removes user from get-all`

Universal framework rules are in `llm.md` in the MTO repo root.
The rules below are specific to this project.

---

## Project layout

```
tests/src/test/java/org/mtodemo/tests/
  users/
    endpoint/   ← single endpoint tests (/gen-endpoint-test)
    flow/       ← multi-step sequence tests (this skill)
    component/  ← vertical slice tests (/gen-component-test)
    UserEndToEndTest.java  ← full cross-system chain
```

## Package

`org.mtodemo.tests.users.flow`

## Test class name convention

`<Domain>FlowTest` — e.g. `UserFlowTest`

## Naming convention

`verbNoun_thenVerbNoun_outcome`

| Example | Meaning |
|---|---|
| `createThenGet_userRetrievable` | create followed by GET returns the same data |
| `createThenUpdate_thenGet_returnsUpdatedData` | update is reflected in subsequent GET |
| `createThenDelete_thenGet_returns404` | deleted user is no longer retrievable |
| `createThenDelete_thenGetAll_doesNotContainUser` | deleted user absent from list |

## Test structure

Each operation in the flow is a **separate pipeline**. State (the UUID, the DTO) is carried via local variables between pipelines.

```java
@Test(description = "Created user can be retrieved by id")
public void createThenGet_userRetrievable() throws Exception {
    UserDto created = Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .execute();

    Pipeline.given(TestUserRequests.getUser(created.getUuid()))
            .then(httpClient.makeCall(200, UserDto.class))
            .then(Verify.matching(created))
            .execute();
}
```

```java
@Test(description = "Updated user data is reflected in subsequent GET")
public void createThenUpdate_thenGet_returnsUpdatedData() throws Exception {
    UserDto created = Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .execute();

    UserDto updatePayload = TestUsers.defaultUser();
    Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
            .then(httpClient.makeCall(200, UserDto.class))
            .execute();

    Pipeline.given(TestUserRequests.getUser(created.getUuid()))
            .then(httpClient.makeCall(200, UserDto.class))
            .then(Verify.matching(updatePayload))
            .execute();
}
```

```java
@Test(description = "Deleted user is no longer retrievable by id")
public void createThenDelete_thenGet_returns404() throws Exception {
    UserDto created = Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .execute();

    Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
            .then(httpClient.makeCall(204, Void.class))
            .execute();

    Pipeline.given(TestUserRequests.getUser(created.getUuid()))
            .then(httpClient.makeCall(404, ErrorDto.class))
            .then(Verify.matching(ErrorDto.withStatus(404)))
            .execute();
}
```

```java
@Test(description = "Deleted user is no longer present in the full user list")
public void createThenDelete_thenGetAll_doesNotContainUser() throws Exception {
    UserDto created = Pipeline.given(TestUserRequests.createUser())
            .then(httpClient.makeCall(201, UserDto.class))
            .execute();

    Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
            .then(httpClient.makeCall(204, Void.class))
            .execute();

    List<UserDto> all = Pipeline.given(TestUserRequests.getAllUsers())
            .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
            .execute();

    assertThat(all).usingRecursiveFieldByFieldElementComparator()
            .doesNotContain(created);
}
```

## Rules

- **No validation / error body tests** — blank/null/size tests belong in endpoint tests, not here
- **Focus on state transitions**: does the system reflect the change correctly in subsequent reads?
- **One pipeline per operation** — do not chain create→update in a single pipeline just because you can; separate pipelines make the flow readable as a sequence of steps
- **Carry state via local variables** — the created UUID or DTO is assigned to a variable and reused in subsequent pipelines
- **Use `Verify.matching`** for response body checks; use `Verify.containsAll` for list inclusion; use `assertThat(...).doesNotContain(...)` for list exclusion
- **description** should read as a user story: "Created user can be retrieved by id", not "Test create then get"
- **Do not re-test persistence in DB** — that is covered by endpoint tests; flow tests only verify API-level consistency

## Imports reference

```java
import com.fasterxml.jackson.core.type.TypeReference;
import org.assertj.core.api.Assertions.assertThat;
import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.dto.ErrorDto;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.factory.TestUsers;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.testng.annotations.Test;
import java.util.List;
```

## Available request factories

```java
TestUserRequests.createUser()
TestUserRequests.createUser(UserDto dto)
TestUserRequests.updateUser(UUID id, UserDto dto)
TestUserRequests.getUser(UUID id)
TestUserRequests.getAllUsers()
TestUserRequests.deleteUser(UUID id)
```

---

Now generate the flow tests described in the argument: **$ARGUMENTS**
