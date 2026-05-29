package org.mtodemo.userservice.dto;

import java.util.UUID;

public record CarDto(
        UUID uuid,
        String plateNumber,
        String manufacturer,
        String model
) {}
