package org.mtodemo.tests.users.endpoint;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.http.model.HttpCallResponse;
import org.mtodemo.tests.dto.ErrorDto;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.infrastructure.BaseTest;
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

    @Test(description = "Request with wrong token returns 401")
    public void createUser_wrongToken_returns401() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.withBearer("wrong-token")
                        .makeCall(new HttpCallResponse<>(401, Map.of(), ErrorDto.withStatus(401))))
                .execute();
    }
}
