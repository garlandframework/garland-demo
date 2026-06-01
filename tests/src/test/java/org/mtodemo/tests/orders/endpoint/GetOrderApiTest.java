package org.mtodemo.tests.orders.endpoint;

import com.fasterxml.jackson.core.type.TypeReference;
import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.dto.ErrorDto;
import org.mtodemo.tests.dto.OrderDto;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.factory.TestOrderRequests;
import org.mtodemo.tests.factory.TestOrders;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class GetOrderApiTest extends BaseTest {

    // --- GET /api/orders/{id} ---

    @Test(description = "GET /api/orders/{id} with non-existent id returns 404 with error body")
    public void getOrderById_notFound() {
        Pipeline.given(TestOrderRequests.getOrder(UUID.randomUUID()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .then(Verify.matching(ErrorDto.withStatus(404)))
                .execute();
    }

    @Test(description = "GET /api/orders/{id} returns 200 and the order data matching the placed order")
    public void getOrderById_returnsCorrectOrder() {
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

    // --- GET /api/orders/user/{userId} ---

    @Test(description = "GET /api/orders/user/{userId} returns 200 and list containing orders placed by that user")
    public void getOrdersByUser_returnsPlacedOrders() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        OrderDto first = Pipeline.given(TestOrderRequests.placeOrder(
                        TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .execute();

        OrderDto second = Pipeline.given(TestOrderRequests.placeOrder(
                        TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .execute();

        Pipeline.given(TestOrderRequests.getOrdersByUser(user.getUuid()))
                .then(httpClient.makeCall(200, new TypeReference<List<OrderDto>>() {}))
                .then(Verify.containsAll(List.of(first, second)))
                .execute();
    }

    @Test(description = "GET /api/orders/user/{userId} with a userId that has no orders returns 200 and empty list")
    public void getOrdersByUser_unknownUser_returnsEmptyList() {
        List<OrderDto> orders = Pipeline.given(TestOrderRequests.getOrdersByUser(UUID.randomUUID()))
                .then(httpClient.makeCall(200, new TypeReference<List<OrderDto>>() {}))
                .execute();

        assertThat(orders).isEmpty();
    }
}
