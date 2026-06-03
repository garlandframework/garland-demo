package org.mtodemo.tests.support.orders.document;

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
public class OrderProjectionDoc {
    private UUID id;
    private UUID userId;
    private List<OrderItemDoc> items;
    private String status;
    private Instant eventTimestamp;
    private String sourceSystem;
}
