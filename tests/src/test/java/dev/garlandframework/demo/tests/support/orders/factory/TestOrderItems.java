package dev.garlandframework.demo.tests.support.orders.factory;

import net.datafaker.Faker;
import dev.garlandframework.demo.tests.support.orders.dto.OrderItemDto;

import java.math.BigDecimal;

public final class TestOrderItems {

    private static final Faker faker = new Faker();

    private TestOrderItems() {}

    public static OrderItemDto defaultOrderItem() {
        return builder().build();
    }

    public static OrderItemDto.OrderItemDtoBuilder builder() {
        return OrderItemDto.builder()
                .productName(faker.commerce().productName())
                .quantity(faker.number().numberBetween(1, 10))
                .unitPrice(BigDecimal.valueOf(faker.number().randomDouble(2, 1, 1000)));
    }
}
