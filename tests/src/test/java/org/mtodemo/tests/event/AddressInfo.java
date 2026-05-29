package org.mtodemo.tests.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AddressInfo(
        UUID addressId,
        String streetName,
        String cityName,
        String countryCode,
        String postalCode
) {}
