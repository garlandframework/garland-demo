package dev.garlandframework.demo.userservice.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemInfo(
        UUID itemId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {}
