package org.mtodemo.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CarRequest(
        @NotBlank @Size(max = 20)  String plateNumber,
        @NotBlank @Size(max = 100) String manufacturer,
        @NotBlank @Size(max = 100) String model
) {}
