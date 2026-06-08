package dev.garlandframework.demo.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank @Size(max = 200) String productName,
        @NotNull @Positive Integer quantity,
        @NotNull @Positive BigDecimal unitPrice
) {}
