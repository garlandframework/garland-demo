package org.mtodemo.tests.support.users.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProjectionDoc {

    private UUID id;
    private String fullName;
    private AddressInfoDoc addressInfo;
    private List<VehicleInfoDoc> vehicleList;
    private Instant eventTimestamp;
    private String sourceSystem;
}
