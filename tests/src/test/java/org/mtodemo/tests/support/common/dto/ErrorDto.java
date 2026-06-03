package org.mtodemo.tests.support.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorDto {
    private int status;
    private String message;

    public static ErrorDto withStatus(int status) {
        return new ErrorDto(status, null);
    }
}
