package org.mtodemo.tests.users.flow;

import com.fasterxml.jackson.core.type.TypeReference;
import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.common.dto.ErrorDto;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.mtodemo.tests.support.users.factory.TestUsers;
import org.testng.annotations.Test;

import java.util.List;


@Test(description = "Flow tests for the users domain: CRUD lifecycle, list containment after create, list exclusion after delete")
public class UserFlowTest extends BaseTest {

    // --- read consistency ---

    @Test(description = "Created user can be retrieved by id")
    public void createThenGet_userRetrievable() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.getUser(created.getUuid()))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(Verify.matching(created))
                .execute();
    }

    @Test(description = "Created user appears in the full user list")
    public void createThenGetAll_listContainsUser() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.getAllUsers())
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .then(Verify.containsAll(List.of(created)))
                .execute();
    }

    @Test(description = "Updated user data is reflected in subsequent GET by id")
    public void createThenUpdate_thenGet_returnsUpdatedData() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
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

    @Test(description = "Updated user data is reflected in the full user list")
    public void createThenUpdate_thenGetAll_listContainsUpdatedData() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserDto updatePayload = TestUsers.defaultUser();
        UserDto updated = Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
                .then(httpClient.makeCall(200, UserDto.class))
                .execute();

        Pipeline.given(TestUserRequests.getAllUsers())
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .then(Verify.containsAll(List.of(updated)))
                .execute();
    }

    // --- delete flows ---

    @Test(description = "Deleted user is no longer retrievable by id")
    public void createThenDelete_thenGet_returns404() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(TestUserRequests.getUser(created.getUuid()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .then(Verify.matching(ErrorDto.withStatus(404)))
                .execute();
    }

    @Test(description = "Deleted user is no longer present in the full user list")
    public void createThenDelete_thenGetAll_doesNotContainUser() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(TestUserRequests.getAllUsers())
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .then(Verify.doesNotContain(List.of(created)))
                .execute();
    }

    @Test(description = "Deleting an already deleted user returns 404")
    public void createThenDelete_thenDeleteAgain_returns404() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .then(Verify.matching(ErrorDto.withStatus(404)))
                .execute();
    }

    // --- update isolation ---

    @Test(description = "Updating one user does not affect another user's data")
    public void createTwo_updateOne_thenGetOther_unchanged() {
        UserDto userA = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserDto userB = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.updateUser(userA.getUuid(), TestUsers.defaultUser()))
                .then(httpClient.makeCall(200, UserDto.class))
                .execute();

        Pipeline.given(TestUserRequests.getUser(userB.getUuid()))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(Verify.matching(userB))
                .execute();
    }

    @Test(description = "Deleting one user does not remove other users from the list")
    public void createTwo_deleteOne_thenGetAll_otherStillPresent() {
        UserDto userA = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserDto userB = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.deleteUser(userA.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(TestUserRequests.getAllUsers())
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .then(Verify.containsAll(List.of(userB)))
                .execute();
    }

    // --- full lifecycle ---

    @Test(description = "Full user lifecycle: create, read, update, read, list, delete, verify gone from read and list")
    public void fullCrudLifecycle() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.getUser(created.getUuid()))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(Verify.matching(created))
                .execute();

        UserDto updatePayload = TestUsers.defaultUser();
        UserDto updated = Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
                .then(httpClient.makeCall(200, UserDto.class))
                .execute();

        Pipeline.given(TestUserRequests.getUser(created.getUuid()))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(Verify.matching(updated))
                .execute();

        Pipeline.given(TestUserRequests.getAllUsers())
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .then(Verify.containsAll(List.of(updated)))
                .execute();

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(TestUserRequests.getUser(created.getUuid()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .then(Verify.matching(ErrorDto.withStatus(404)))
                .execute();

        Pipeline.given(TestUserRequests.getAllUsers())
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .then(Verify.doesNotContain(List.of(created)))
                .execute();
    }
}
