package dev.garlandframework.demo.tests.support.users.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserUpdatedEvent(
        UUID userId,
        String fullName,
        AddressInfo addressInfo,
        List<VehicleInfo> vehicleList,
        Instant eventTimestamp,
        String sourceSystem
) {}
