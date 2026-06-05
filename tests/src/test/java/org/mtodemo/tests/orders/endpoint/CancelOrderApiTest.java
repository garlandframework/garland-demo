package org.mtodemo.tests.orders.endpoint;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.common.dto.ErrorDto;
import org.mtodemo.tests.support.orders.dto.OrderDto;
import org.mtodemo.tests.support.orders.dto.OrderStatus;
import org.mtodemo.tests.support.orders.factory.TestOrderRequests;
import org.mtodemo.tests.support.orders.factory.TestOrders;
import org.mtodemo.tests.support.orders.mapper.OrderTestMapper;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.testng.annotations.Test;

import java.util.UUID;

@Test(description = "Endpoint tests for PUT /api/orders/{id}/cancel: not found and successful cancellation with Postgres verification")
public class CancelOrderApiTest extends BaseTest {

    @Test(description = "PUT /api/orders/{id}/cancel with non-existent id returns 404 with error body")
    public void cancelOrder_notFound() {
        Pipeline.given(TestOrderRequests.cancelOrder(UUID.randomUUID()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .then(Verify.matching(ErrorDto.withStatus(404)))
                .execute();
    }

    @Test(description = "PUT /api/orders/{id}/cancel returns 200 with CANCELLED status and change is persisted in Postgres")
    public void cancelOrder_statusUpdatedInDb() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        OrderDto created = Pipeline.given(TestOrderRequests.placeOrder(
                        TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(trackOrder())
                .execute();

        OrderDto expectedCancelled = OrderDto.builder()
                .uuid(created.getUuid())
                .userId(created.getUserId())
                .status(OrderStatus.CANCELLED)
                .build();
        Pipeline.given(TestOrderRequests.cancelOrder(created.getUuid()))
                .then(httpClient.makeCall(200, OrderDto.class))
                .then(Verify.matching(expectedCancelled))
                .then(OrderTestMapper.toEntity())
                .then(postgresClient.findById())
                .execute();
    }
}
