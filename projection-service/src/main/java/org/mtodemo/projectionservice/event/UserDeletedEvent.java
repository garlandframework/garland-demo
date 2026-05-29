package org.mtodemo.projectionservice.event;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedEvent(
        UUID userId,
        Instant eventTimestamp
) {}
