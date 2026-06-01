package org.mtodemo.tests.factory;

import org.modulartestorchestrator.http.model.HttpCallRequest;
import org.mtodemo.tests.dto.OrderDto;
import org.mtodemo.tests.infrastructure.Connections;

import java.util.List;
import java.util.UUID;

public final class TestOrderRequests {

    private TestOrderRequests() {}

    public static HttpCallRequest<OrderDto> placeOrder() {
        return placeOrder(TestOrders.defaultOrder());
    }

    public static HttpCallRequest<OrderDto> placeOrder(OrderDto dto) {
        return new HttpCallRequest<>(
                Connections.USER_SERVICE_URL + "/api/orders",
                "POST",
                List.of(),
                dto);
    }

    public static HttpCallRequest<Void> getOrder(UUID id) {
        return new HttpCallRequest<>(
                Connections.USER_SERVICE_URL + "/api/orders/" + id,
                "GET",
                List.of(),
                null);
    }

    public static HttpCallRequest<Void> getOrdersByUser(UUID userId) {
        return new HttpCallRequest<>(
                Connections.USER_SERVICE_URL + "/api/orders/user/" + userId,
                "GET",
                List.of(),
                null);
    }

    public static HttpCallRequest<Void> cancelOrder(UUID id) {
        return new HttpCallRequest<>(
                Connections.USER_SERVICE_URL + "/api/orders/" + id + "/cancel",
                "PUT",
                List.of(),
                null);
    }
}
