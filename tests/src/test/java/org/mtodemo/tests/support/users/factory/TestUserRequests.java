package org.mtodemo.tests.support.users.factory;

import org.modulartestorchestrator.http.model.HttpCallRequest;
import org.mtodemo.tests.support.base.Connections;
import org.mtodemo.tests.support.users.dto.UserDto;

import java.util.UUID;

public final class TestUserRequests {

    private TestUserRequests() {}

    public static HttpCallRequest<UserDto> createUser() {
        return createUser(TestUsers.defaultUser());
    }

    public static HttpCallRequest<UserDto> createUser(UserDto dto) {
        return HttpCallRequest.post(Connections.USER_SERVICE_URL + "/api/users", dto);
    }

    public static HttpCallRequest<UserDto> updateUser(UUID id, UserDto dto) {
        return HttpCallRequest.put(Connections.USER_SERVICE_URL + "/api/users/" + id, dto);
    }

    public static HttpCallRequest<Void> getAllUsers() {
        return HttpCallRequest.get(Connections.USER_SERVICE_URL + "/api/users");
    }

    public static HttpCallRequest<Void> getUser(UUID id) {
        return HttpCallRequest.get(Connections.USER_SERVICE_URL + "/api/users/" + id);
    }

    public static HttpCallRequest<Void> deleteUser(UUID id) {
        return HttpCallRequest.delete(Connections.USER_SERVICE_URL + "/api/users/" + id);
    }

    public static HttpCallRequest<Void> exportUser(UUID id) {
        return HttpCallRequest.get(Connections.USER_SERVICE_URL + "/api/users/" + id + "/export");
    }
}
