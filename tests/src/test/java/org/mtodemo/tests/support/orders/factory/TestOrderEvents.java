package org.mtodemo.tests.support.orders.factory;

import net.datafaker.Faker;
import org.mtodemo.tests.support.orders.event.OrderItemInfo;
import org.mtodemo.tests.support.orders.event.OrderPlacedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TestOrderEvents {

    private static final Faker faker = new Faker();

    private TestOrderEvents() {}

    public static OrderPlacedEvent defaultOrderPlacedEvent() {
        return new OrderPlacedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new OrderItemInfo(
                        UUID.randomUUID(),
                        faker.commerce().productName(),
                        faker.number().numberBetween(1, 10),
                        BigDecimal.valueOf(faker.number().randomDouble(2, 1, 1000))
                )),
                Instant.now(),
                "user-service"
        );
    }
}
