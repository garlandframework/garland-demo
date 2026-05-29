package org.mtodemo.tests.factory;

import net.datafaker.Faker;
import org.mtodemo.tests.dto.AddressDto;

public final class TestAddresses {

    private static final Faker faker = new Faker();

    private TestAddresses() {}

    public static AddressDto defaultAddress() {
        return builder().build();
    }

    public static AddressDto.AddressDtoBuilder builder() {
        return AddressDto.builder()
                .street(faker.address().streetAddress())
                .city(faker.address().city())
                .country(faker.address().country())
                .zipCode(faker.address().zipCode());
    }
}
