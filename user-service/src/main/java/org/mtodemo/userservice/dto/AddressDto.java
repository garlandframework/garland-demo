package org.mtodemo.userservice.dto;

import java.util.UUID;

public record AddressDto(
        UUID uuid,
        String street,
        String city,
        String country,
        String zipCode
) {}
