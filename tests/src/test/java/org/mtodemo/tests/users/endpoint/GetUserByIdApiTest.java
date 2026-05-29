package org.mtodemo.tests.users.endpoint;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.dto.ErrorDto;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.testng.annotations.Test;

import java.util.UUID;

public class GetUserByIdApiTest extends BaseTest {

    @Test(description = "GET /api/users/{id} with non-existent id returns 404 with error body")
    public void getUserById_notFound() throws Exception {
        Pipeline.given(TestUserRequests.getUser(UUID.randomUUID()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .then(Verify.matching(ErrorDto.withStatus(404)))
                .execute();
    }

    @Test(description = "GET /api/users/{id} returns 200 and the user data matching the given id")
    public void getUserById_returnsCorrectUser() throws Exception {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        Pipeline.given(TestUserRequests.getUser(created.getUuid()))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(Verify.matching(created))
                .execute();
    }
}
