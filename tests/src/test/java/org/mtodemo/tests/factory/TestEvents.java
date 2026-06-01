package org.mtodemo.tests.factory;

import net.datafaker.Faker;
import org.mtodemo.tests.event.AddressInfo;
import org.mtodemo.tests.event.OrderItemInfo;
import org.mtodemo.tests.event.OrderPlacedEvent;
import org.mtodemo.tests.event.UserCreatedEvent;
import org.mtodemo.tests.event.VehicleInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TestEvents {

    private static final Faker faker = new Faker();

    private TestEvents() {}

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

    public static UserCreatedEvent defaultUserCreatedEvent() {
        return new UserCreatedEvent(
                UUID.randomUUID(),
                faker.name().firstName() + " " + faker.name().lastName(),
                new AddressInfo(
                        UUID.randomUUID(),
                        faker.address().streetAddress(),
                        faker.address().city(),
                        faker.address().country(),
                        faker.address().zipCode()
                ),
                List.of(new VehicleInfo(
                        UUID.randomUUID(),
                        faker.vehicle().licensePlate(),
                        faker.vehicle().make(),
                        faker.vehicle().model()
                )),
                Instant.now(),
                "user-service"
        );
    }
}
