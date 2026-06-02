package org.mtodemo.tests.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.modulartestorchestrator.base.Step;
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

    static Step<OrderDto, OrderEntity> toEntity() {
        return Step.lift(INSTANCE::toEntity);
    }

    static Step<OrderDto, OrderPlacedEvent> toPlacedEvent() {
        return Step.lift(INSTANCE::toPlacedEvent);
    }

    static Step<OrderEntity, OrderPlacedEvent> entityToPlacedEvent() {
        return Step.lift(INSTANCE::entityToPlacedEvent);
    }

    static Step<OrderPlacedEvent, OrderProjectionDoc> toProjectionDoc() {
        return Step.lift(INSTANCE::toProjectionDoc);
    }

    static Step<OrderDto, OrderCancelledEvent> toCancelledEvent() {
        return Step.lift(INSTANCE::toCancelledEvent);
    }

    static Step<OrderDto, OrderProjectionDoc> toCancelledProjectionDoc() {
        return Step.lift(dto -> OrderProjectionDoc.builder()
                .id(dto.getUuid())
                .status("CANCELLED")
                .build());
    }
}
