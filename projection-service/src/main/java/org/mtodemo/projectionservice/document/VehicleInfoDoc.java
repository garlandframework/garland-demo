package dev.garlandframework.demo.projectionservice.document;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleInfoDoc {
    private UUID vehicleId;
    private String licensePlate;
    private String make;
    private String vehicleModel;
}
