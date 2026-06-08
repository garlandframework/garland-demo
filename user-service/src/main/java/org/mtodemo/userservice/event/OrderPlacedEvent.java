package dev.garlandframework.demo.userservice.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        UUID userId,
        List<OrderItemInfo> items,
        Instant eventTimestamp,
        String sourceSystem
) {}
