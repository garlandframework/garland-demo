package org.mtodemo.userservice.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID uuid,
        UUID userId,
        List<OrderItemDto> items,
        OrderStatus status,
        LocalDateTime createdAt
) {}
