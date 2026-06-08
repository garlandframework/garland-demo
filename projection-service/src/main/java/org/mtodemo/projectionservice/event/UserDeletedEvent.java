package dev.garlandframework.demo.projectionservice.event;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEvent(
        UUID userId,
        Instant eventTimestamp
) {}
