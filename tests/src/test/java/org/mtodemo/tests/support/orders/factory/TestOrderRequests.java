package org.mtodemo.tests.support.orders.factory;

import org.modulartestorchestrator.http.model.HttpCallRequest;
import org.mtodemo.tests.support.base.Connections;
import org.mtodemo.tests.support.orders.dto.OrderDto;

import java.util.UUID;

public final class TestOrderRequests {

    private TestOrderRequests() {}

    public static HttpCallRequest<OrderDto> placeOrder() {
        return placeOrder(TestOrders.defaultOrder());
    }

    public static HttpCallRequest<OrderDto> placeOrder(OrderDto dto) {
        return HttpCallRequest.post(Connections.USER_SERVICE_URL + "/api/orders", dto);
    }

    public static HttpCallRequest<Void> getOrder(UUID id) {
        return HttpCallRequest.get(Connections.USER_SERVICE_URL + "/api/orders/" + id);
    }

    public static HttpCallRequest<Void> getOrdersByUser(UUID userId) {
        return HttpCallRequest.get(Connections.USER_SERVICE_URL + "/api/orders/user/" + userId);
    }

    public static HttpCallRequest<Void> cancelOrder(UUID id) {
        return HttpCallRequest.put(Connections.USER_SERVICE_URL + "/api/orders/" + id + "/cancel");
    }
}
