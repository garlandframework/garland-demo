package org.mtodemo.tests.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationErrorDto {
    private int status;
    private List<FieldViolationDto> errors;

    public static ValidationErrorDto forField(String field) {
        return new ValidationErrorDto(400, List.of(new FieldViolationDto(field, null)));
    }
}
