package org.mtodemo.tests.users.endpoint;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.http.model.HttpCallResponse;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.common.dto.ErrorDto;
import org.mtodemo.tests.support.common.factory.TestAuthRequests;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.testng.annotations.Test;

import java.util.Map;

public class AuthTest extends BaseTest {

    @Test(description = "Request without Authorization header returns 401")
    public void createUser_noToken_returns401() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.withoutHeader("Authorization")
                        .makeCall(new HttpCallResponse<>(401, Map.of(), ErrorDto.withStatus(401))))
                .execute();
    }

    @Test(description = "Request with invalid JWT returns 401")
    public void createUser_invalidToken_returns401() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.withBearer("not-a-valid-jwt")
                        .makeCall(new HttpCallResponse<>(401, Map.of(), ErrorDto.withStatus(401))))
                .execute();
    }

    @Test(description = "Login with wrong credentials returns 401")
    public void login_wrongCredentials_returns401() {
        Pipeline.given(TestAuthRequests.login("admin", "wrong-password"))
                .then(httpClient.makeCall(new HttpCallResponse<>(401, Map.of(), ErrorDto.withStatus(401))))
                .execute();
    }
}
