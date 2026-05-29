package org.mtodemo.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mtodemo.userservice.entity.OrderEntity;
import org.mtodemo.userservice.entity.OrderItemEntity;
import org.mtodemo.userservice.event.OrderCancelledEvent;
import org.mtodemo.userservice.event.OrderItemInfo;
import org.mtodemo.userservice.event.OrderPlacedEvent;

@Mapper(componentModel = "spring")
public interface OrderEventMapper {

    @Mapping(source = "id", target = "orderId")
    @Mapping(target = "eventTimestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "sourceSystem", constant = "user-service")
    OrderPlacedEvent toPlacedEvent(OrderEntity entity);

    @Mapping(source = "id", target = "itemId")
    OrderItemInfo toItemInfo(OrderItemEntity entity);

    @Mapping(source = "id", target = "orderId")
    @Mapping(target = "eventTimestamp", expression = "java(java.time.Instant.now())")
    OrderCancelledEvent toCancelledEvent(OrderEntity entity);
}
