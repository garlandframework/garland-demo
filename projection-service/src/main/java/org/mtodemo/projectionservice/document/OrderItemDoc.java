package org.mtodemo.projectionservice.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDoc {
    private UUID itemId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
