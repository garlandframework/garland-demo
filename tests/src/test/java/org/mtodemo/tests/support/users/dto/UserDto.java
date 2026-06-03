package org.mtodemo.tests.support.users.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {
    private UUID uuid;
    private String name;
    private String surname;
    private AddressDto address;
    private List<CarDto> cars;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
