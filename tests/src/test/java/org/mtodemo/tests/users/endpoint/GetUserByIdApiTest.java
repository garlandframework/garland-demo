package org.mtodemo.tests.users.endpoint;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.common.dto.ErrorDto;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
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
