package org.mtodemo.tests.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleInfo(
        UUID vehicleId,
        String licensePlate,
        String make,
        String vehicleModel
) {}
