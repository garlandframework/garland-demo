package dev.garlandframework.demo.tests.support.orders.factory;

import dev.garlandframework.demo.tests.support.orders.dto.OrderDto;

import java.util.List;
import java.util.UUID;

public final class TestOrders {

    public static final UUID PLACEHOLDER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TestOrders() {}

    public static OrderDto defaultOrder() {
        return builder().build();
    }

    public static OrderDto.OrderDtoBuilder builder() {
        return OrderDto.builder()
                .userId(PLACEHOLDER_USER_ID)
                .items(List.of(TestOrderItems.defaultOrderItem()));
    }
}
