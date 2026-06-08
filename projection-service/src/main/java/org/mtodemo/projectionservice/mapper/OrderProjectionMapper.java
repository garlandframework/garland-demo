package dev.garlandframework.demo.projectionservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import dev.garlandframework.demo.projectionservice.document.OrderItemDoc;
import dev.garlandframework.demo.projectionservice.document.OrderProjectionDocument;
import dev.garlandframework.demo.projectionservice.event.OrderItemInfo;
import dev.garlandframework.demo.projectionservice.event.OrderPlacedEvent;

@Mapper(componentModel = "spring")
public interface OrderProjectionMapper {

    @Mapping(source = "orderId", target = "id")
    @Mapping(target = "status", constant = "PENDING")
    OrderProjectionDocument toDocument(OrderPlacedEvent event);

    OrderItemDoc toItemDoc(OrderItemInfo item);
}
