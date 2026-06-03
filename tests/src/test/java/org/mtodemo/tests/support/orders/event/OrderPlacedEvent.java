package org.mtodemo.tests.support.orders.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPlacedEvent(
        UUID orderId,
        UUID userId,
        List<OrderItemInfo> items,
        Instant eventTimestamp,
        String sourceSystem
) {}
