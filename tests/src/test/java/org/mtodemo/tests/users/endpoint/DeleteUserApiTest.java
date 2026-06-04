package org.mtodemo.tests.users.endpoint;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.common.dto.ErrorDto;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.mtodemo.tests.support.users.mapper.UserTestMapper;
import org.testng.annotations.Test;

import java.util.UUID;

public class DeleteUserApiTest extends BaseTest {

    @Test(description = "DELETE /api/users/{id} with non-existent id returns 404 with error body")
    public void deleteUser_notFound() {
        Pipeline.given(TestUserRequests.deleteUser(UUID.randomUUID()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .then(Verify.matching(ErrorDto.withStatus(404)))
                .execute();
    }

    @Test(description = "DELETE /api/users/{id} returns 204 and user is no longer present in Postgres")
    public void deleteUser_removedFromDb() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(created)
                .then(UserTestMapper.toEntity())
                .then(postgresClient.notExistsById())
                .execute();
    }
}
