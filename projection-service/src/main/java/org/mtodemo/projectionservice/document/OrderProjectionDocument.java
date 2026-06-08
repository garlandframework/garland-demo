package dev.garlandframework.demo.projectionservice.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "order_projections")
public class OrderProjectionDocument {

    @Id
    private UUID id;
    private UUID userId;
    private List<OrderItemDoc> items;
    private String status;
    private Instant eventTimestamp;
    private String sourceSystem;
}
