package org.mtodemo.tests.support.users.factory;

import net.datafaker.Faker;
import org.mtodemo.tests.support.users.event.AddressInfo;
import org.mtodemo.tests.support.users.event.UserCreatedEvent;
import org.mtodemo.tests.support.users.event.VehicleInfo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TestUserEvents {

    private static final Faker faker = new Faker();

    private TestUserEvents() {}

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
