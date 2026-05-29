package org.mtodemo.projectionservice.document;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressInfoDoc {
    private UUID addressId;
    private String streetName;
    private String cityName;
    private String countryCode;
    private String postalCode;
}
