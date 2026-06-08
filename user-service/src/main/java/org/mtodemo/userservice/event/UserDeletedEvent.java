package dev.garlandframework.demo.userservice.event;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEvent(
        UUID userId,
        Instant eventTimestamp
) {}
