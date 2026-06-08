package dev.garlandframework.demo.projectionservice.event;

import java.util.UUID;

public record AddressInfo(
        UUID addressId,
        String streetName,
        String cityName,
        String countryCode,
        String postalCode
) {}
