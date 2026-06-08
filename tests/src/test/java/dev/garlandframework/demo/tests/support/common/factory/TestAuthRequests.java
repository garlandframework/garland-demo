package dev.garlandframework.demo.tests.support.common.factory;

import dev.garlandframework.http.model.FormBody;
import dev.garlandframework.http.model.HttpCallRequest;
import dev.garlandframework.demo.tests.support.base.Connections;
import dev.garlandframework.demo.tests.support.common.dto.LoginRequest;

public class TestAuthRequests {

    private static final String BASE_URL = Connections.USER_SERVICE_URL + "/api/auth";

    public static HttpCallRequest<LoginRequest> login() {
        return login(Connections.ADMIN_USERNAME, Connections.ADMIN_PASSWORD);
    }

    public static HttpCallRequest<LoginRequest> login(String username, String password) {
        return HttpCallRequest.post(BASE_URL + "/login", new LoginRequest(username, password));
    }

    public static HttpCallRequest<FormBody> oauthToken() {
        return HttpCallRequest.post(
                Connections.USER_SERVICE_URL + "/oauth/token",
                new FormBody()
                        .field("grant_type", "client_credentials")
                        .field("client_id", Connections.ADMIN_USERNAME)
                        .field("client_secret", Connections.ADMIN_PASSWORD));
    }
}
