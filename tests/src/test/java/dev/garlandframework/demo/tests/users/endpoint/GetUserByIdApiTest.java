package dev.garlandframework.demo.tests.users.endpoint;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.common.dto.ErrorDto;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
import org.testng.annotations.Test;

import java.util.UUID;

@Test(description = "Endpoint tests for GET /api/users/{id}: not found and successful retrieval")
public class GetUserByIdApiTest extends BaseTest {

    @Test(description = "GET /api/users/{id} with non-existent id returns 404 with error body")
    public void getUserById_notFound() {
        Pipeline.given(TestUserRequests.getUser(UUID.randomUUID()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .then(Verify.matching(ErrorDto.withStatus(404)))
                .execute();
    }

    @Test(description = "GET /api/users/{id} returns 200 and the user data matching the given id")
    public void getUserById_returnsCorrectUser() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.getUser(created.getUuid()))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(Verify.matching(created))
                .execute();
    }
}
