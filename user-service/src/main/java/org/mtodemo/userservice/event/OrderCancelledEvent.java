package dev.garlandframework.demo.userservice.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        UUID userId,
        Instant eventTimestamp
) {}
