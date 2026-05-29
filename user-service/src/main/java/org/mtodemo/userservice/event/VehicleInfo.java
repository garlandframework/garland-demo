package org.mtodemo.userservice.event;

import java.util.UUID;

public record VehicleInfo(
        UUID vehicleId,
        String licensePlate,
        String make,
        String vehicleModel
) {}
