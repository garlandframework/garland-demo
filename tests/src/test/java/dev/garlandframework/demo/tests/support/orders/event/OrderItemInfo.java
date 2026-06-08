package dev.garlandframework.demo.tests.support.orders.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderItemInfo(
        UUID itemId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {}
