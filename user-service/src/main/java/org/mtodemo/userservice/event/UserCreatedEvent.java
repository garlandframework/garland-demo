package dev.garlandframework.demo.userservice.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserCreatedEvent(
        UUID userId,
        String fullName,
        AddressInfo addressInfo,
        List<VehicleInfo> vehicleList,
        Instant eventTimestamp,
        String sourceSystem
) {}
