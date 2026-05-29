package org.mtodemo.tests.factory;

import org.modulartestorchestrator.http.model.HttpCallRequest;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.infrastructure.Connections;

import java.util.List;
import java.util.UUID;

public final class TestUserRequests {

    private TestUserRequests() {}

    public static HttpCallRequest<UserDto> createUser() {
        return createUser(TestUsers.defaultUser());
    }

    public static HttpCallRequest<UserDto> createUser(UserDto dto) {
        return new HttpCallRequest<>(
                Connections.USER_SERVICE_URL + "/api/users",
                "POST",
                List.of(),
                dto);
    }

    public static HttpCallRequest<UserDto> updateUser(UUID id, UserDto dto) {
        return new HttpCallRequest<>(
                Connections.USER_SERVICE_URL + "/api/users/" + id,
                "PUT",
                List.of(),
                dto);
    }

    public static HttpCallRequest<Void> getAllUsers() {
        return new HttpCallRequest<>(
                Connections.USER_SERVICE_URL + "/api/users",
                "GET",
                List.of(),
                null);
    }

    public static HttpCallRequest<Void> getUser(UUID id) {
        return new HttpCallRequest<>(
                Connections.USER_SERVICE_URL + "/api/users/" + id,
                "GET",
                List.of(),
                null);
    }

    public static HttpCallRequest<Void> deleteUser(UUID id) {
        return new HttpCallRequest<>(
                Connections.USER_SERVICE_URL + "/api/users/" + id,
                "DELETE",
                List.of(),
                null);
    }
}
