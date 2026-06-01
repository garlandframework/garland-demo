package org.mtodemo.tests.orders.flow;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.dto.OrderDto;
import org.mtodemo.tests.dto.OrderStatus;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.factory.TestOrderRequests;
import org.mtodemo.tests.factory.TestOrders;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.testng.annotations.Test;

public class OrderFlowTest extends BaseTest {

    @Test(description = "Placed order can be retrieved by id with all original data intact")
    public void placeThenGetById_orderRetrievable() throws Exception {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        OrderDto created = Pipeline.given(TestOrderRequests.placeOrder(
                        TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .execute();

        Pipeline.given(TestOrderRequests.getOrder(created.getUuid()))
                .then(httpClient.makeCall(200, OrderDto.class))
                .then(Verify.matching(created))
                .execute();
    }

    @Test(description = "Cancelled order is reflected as CANCELLED when retrieved by id")
    public void placeThenCancel_thenGetById_statusIsCancelled() throws Exception {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        OrderDto created = Pipeline.given(TestOrderRequests.placeOrder(
                        TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .execute();

        Pipeline.given(TestOrderRequests.cancelOrder(created.getUuid()))
                .then(httpClient.makeCall(200, OrderDto.class))
                .execute();

        OrderDto expectedCancelled = OrderDto.builder()
                .uuid(created.getUuid())
                .userId(created.getUserId())
                .status(OrderStatus.CANCELLED)
                .build();
        Pipeline.given(TestOrderRequests.getOrder(created.getUuid()))
                .then(httpClient.makeCall(200, OrderDto.class))
                .then(Verify.matching(expectedCancelled))
                .execute();
    }
}
