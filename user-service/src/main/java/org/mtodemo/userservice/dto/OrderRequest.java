package org.mtodemo.userservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotNull UUID userId,
        @NotNull @Size(min = 1) @Valid List<OrderItemRequest> items
) {}
