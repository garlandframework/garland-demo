package org.mtodemo.tests.support.users.factory;

import net.datafaker.Faker;
import org.mtodemo.tests.support.users.dto.UserDto;

import java.util.List;

public final class TestUsers {

    private static final Faker faker = new Faker();

    private TestUsers() {}

    public static UserDto defaultUser() {
        return builder().build();
    }

    public static UserDto requiredFieldsOnlyUser() {
        return UserDto.builder()
                .name(faker.name().firstName())
                .surname(faker.name().lastName())
                .build();
    }

    public static UserDto.UserDtoBuilder builder() {
        return UserDto.builder()
                .name(faker.name().firstName())
                .surname(faker.name().lastName())
                .address(TestAddresses.defaultAddress())
                .cars(List.of(TestCars.defaultCar()));
    }
}
