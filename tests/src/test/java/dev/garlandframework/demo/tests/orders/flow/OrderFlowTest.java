package dev.garlandframework.demo.tests.orders.flow;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.orders.dto.OrderDto;
import dev.garlandframework.demo.tests.support.orders.dto.OrderStatus;
import dev.garlandframework.demo.tests.support.orders.factory.TestOrderRequests;
import dev.garlandframework.demo.tests.support.orders.factory.TestOrders;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
import org.testng.annotations.Test;

@Test(description = "Flow tests for the orders domain: retrieve placed order by id and retrieve cancelled order by id")
public class OrderFlowTest extends BaseTest {

    @Test(description = "Placed order can be retrieved by id with all original data intact")
    public void placeThenGetById_orderRetrievable() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        OrderDto created = Pipeline.given(TestOrderRequests.placeOrder(
                        TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(trackOrder())
                .execute();

        Pipeline.given(TestOrderRequests.getOrder(created.getUuid()))
                .then(httpClient.makeCall(200, OrderDto.class))
                .then(Verify.matching(created))
                .execute();
    }

    @Test(description = "Cancelled order is reflected as CANCELLED when retrieved by id")
    public void placeThenCancel_thenGetById_statusIsCancelled() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        OrderDto created = Pipeline.given(TestOrderRequests.placeOrder(
                        TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(trackOrder())
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
