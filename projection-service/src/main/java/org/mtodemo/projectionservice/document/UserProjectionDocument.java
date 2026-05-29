package org.mtodemo.projectionservice.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProjectionDocument {

    @Id
    private UUID id;

    private String fullName;
    private AddressInfoDoc addressInfo;
    private List<VehicleInfoDoc> vehicleList;
    private Instant eventTimestamp;
    private String sourceSystem;
}
