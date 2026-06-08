package dev.garlandframework.demo.projectionservice.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserUpdatedEvent(
        UUID userId,
        String fullName,
        AddressInfo addressInfo,
        List<VehicleInfo> vehicleList,
        Instant eventTimestamp,
        String sourceSystem
) {}
