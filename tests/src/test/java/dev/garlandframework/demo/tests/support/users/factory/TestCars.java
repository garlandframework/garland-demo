package dev.garlandframework.demo.tests.support.users.factory;

import net.datafaker.Faker;
import dev.garlandframework.demo.tests.support.users.dto.CarDto;

public final class TestCars {

    private static final Faker faker = new Faker();

    private TestCars() {}

    public static CarDto defaultCar() {
        return builder().build();
    }

    public static CarDto.CarDtoBuilder builder() {
        return CarDto.builder()
                .plateNumber(faker.vehicle().licensePlate())
                .manufacturer(faker.vehicle().make())
                .model(faker.vehicle().model());
    }
}
