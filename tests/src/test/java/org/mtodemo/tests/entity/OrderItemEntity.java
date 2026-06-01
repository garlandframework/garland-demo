package org.mtodemo.tests.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemEntity {

    @Id
    private UUID id;

    @Column(name = "product_name")
    private String productName;
    private Integer quantity;
    @Column(name = "unit_price")
    private BigDecimal unitPrice;
}
