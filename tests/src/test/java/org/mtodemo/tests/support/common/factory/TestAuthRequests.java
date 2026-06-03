package org.mtodemo.tests.support.common.factory;

import org.modulartestorchestrator.http.model.HttpCallRequest;
import org.mtodemo.tests.support.base.Connections;
import org.mtodemo.tests.support.common.dto.LoginRequest;

import java.util.List;

public class TestAuthRequests {

    private static final String BASE_URL = Connections.USER_SERVICE_URL + "/api/auth";

    public static HttpCallRequest<LoginRequest> login() {
        return login(Connections.ADMIN_USERNAME, Connections.ADMIN_PASSWORD);
    }

    public static HttpCallRequest<LoginRequest> login(String username, String password) {
        return new HttpCallRequest<>(BASE_URL + "/login", "POST", List.of(), new LoginRequest(username, password));
    }
}
