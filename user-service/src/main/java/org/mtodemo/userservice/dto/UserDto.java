package dev.garlandframework.demo.userservice.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserDto(
        UUID uuid,
        String name,
        String surname,
        AddressDto address,
        List<CarDto> cars,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
