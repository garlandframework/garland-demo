package dev.garlandframework.demo.userservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID uuid,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {}
