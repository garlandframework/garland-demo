package org.mtodemo.tests.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserDeletedEvent(
        UUID userId,
        Instant eventTimestamp
) {}
