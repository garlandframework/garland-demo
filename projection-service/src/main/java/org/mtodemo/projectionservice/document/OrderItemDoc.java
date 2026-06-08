package dev.garlandframework.demo.projectionservice.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDoc {
    private UUID itemId;
    private String productName;
    private Integer quantity;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal unitPrice;
}
