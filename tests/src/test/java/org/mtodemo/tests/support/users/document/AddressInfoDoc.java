package org.mtodemo.tests.support.users.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressInfoDoc {

    private UUID addressId;
    private String streetName;
    private String cityName;
    private String countryCode;
    private String postalCode;
}
