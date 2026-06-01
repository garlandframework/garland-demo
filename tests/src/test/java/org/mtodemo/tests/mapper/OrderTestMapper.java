package org.mtodemo.tests.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.modulartestorchestrator.base.StepFunction;
import org.mtodemo.tests.document.OrderItemDoc;
import org.mtodemo.tests.document.OrderProjectionDoc;
import org.mtodemo.tests.dto.OrderDto;
import org.mtodemo.tests.dto.OrderItemDto;
import org.mtodemo.tests.entity.OrderEntity;
import org.mtodemo.tests.entity.OrderItemEntity;
import org.mtodemo.tests.event.OrderCancelledEvent;
import org.mtodemo.tests.event.OrderItemInfo;
import org.mtodemo.tests.event.OrderPlacedEvent;

@Mapper
public interface OrderTestMapper {

    OrderTestMapper INSTANCE = Mappers.getMapper(OrderTestMapper.class);

    // --- Entity mappings ---

    @Mapping(source = "uuid", target = "id")
    OrderEntity toEntity(OrderDto dto);

    @Mapping(source = "uuid", target = "id")
    OrderItemEntity toEntity(OrderItemDto dto);

    // --- Placed event mappings ---

    @Mapping(source = "uuid", target = "orderId")
    @Mapping(target = "eventTimestamp", ignore = true)
    @Mapping(target = "sourceSystem", constant = "user-service")
    OrderPlacedEvent toPlacedEvent(OrderDto dto);

    @Mapping(source = "uuid", target = "itemId")
    OrderItemInfo toOrderItemInfo(OrderItemDto dto);

    @Mapping(source = "id", target = "orderId")
    @Mapping(target = "eventTimestamp", ignore = true)
    @Mapping(target = "sourceSystem", constant = "user-service")
    OrderPlacedEvent entityToPlacedEvent(OrderEntity entity);

    @Mapping(source = "id", target = "itemId")
    OrderItemInfo toOrderItemInfo(OrderItemEntity entity);

    // --- Cancelled event mappings ---

    @Mapping(source = "uuid", target = "orderId")
    @Mapping(target = "eventTimestamp", ignore = true)
    OrderCancelledEvent toCancelledEvent(OrderDto dto);

    // --- Projection document mappings ---

    @Mapping(source = "orderId", target = "id")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sourceSystem", ignore = true)
    OrderProjectionDoc toProjectionDoc(OrderPlacedEvent event);

    OrderItemDoc toOrderItemDoc(OrderItemInfo info);

    // --- Pipeline bridges ---

    static StepFunction<OrderDto, OrderEntity> toEntity() {
        return StepFunction.lift(INSTANCE::toEntity);
    }

    static StepFunction<OrderDto, OrderPlacedEvent> toPlacedEvent() {
        return StepFunction.lift(INSTANCE::toPlacedEvent);
    }

    static StepFunction<OrderEntity, OrderPlacedEvent> entityToPlacedEvent() {
        return StepFunction.lift(INSTANCE::entityToPlacedEvent);
    }

    static StepFunction<OrderPlacedEvent, OrderProjectionDoc> toProjectionDoc() {
        return StepFunction.lift(INSTANCE::toProjectionDoc);
    }

    static StepFunction<OrderDto, OrderCancelledEvent> toCancelledEvent() {
        return StepFunction.lift(INSTANCE::toCancelledEvent);
    }
}
