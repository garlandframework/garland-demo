package org.mtodemo.tests.users.endpoint;

import com.fasterxml.jackson.core.type.TypeReference;
import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Test(description = "Endpoint tests for GET /api/users: empty list when no users exist and list containing all created users")
public class GetAllUsersApiTest extends BaseTest {

    @Test(description = "GET /api/users returns 200 and empty list when no users exist")
    public void getAllUsers_emptyDatabase_returnsEmptyList() {
        List<UserDto> all = Pipeline.given(TestUserRequests.getAllUsers())
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .execute();

        assertThat(all).isEmpty();
    }

    @Test(description = "GET /api/users returns 200 and the list contains all previously created users")
    public void getAllUsers_containsCreatedUsers() {
        UserDto first = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserDto second = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.getAllUsers())
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .then(Verify.containsAll(List.of(first, second)))
                .execute();
    }
}
