package dev.garlandframework.demo.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import dev.garlandframework.demo.userservice.entity.OrderEntity;
import dev.garlandframework.demo.userservice.entity.OrderItemEntity;
import dev.garlandframework.demo.userservice.event.OrderCancelledEvent;
import dev.garlandframework.demo.userservice.event.OrderItemInfo;
import dev.garlandframework.demo.userservice.event.OrderPlacedEvent;

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
