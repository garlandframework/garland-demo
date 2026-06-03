package org.mtodemo.tests.support.orders.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCancelledEvent(
        UUID orderId,
        UUID userId,
        Instant eventTimestamp
) {}
