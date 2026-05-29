package org.mtodemo.projectionservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mtodemo.projectionservice.document.OrderItemDoc;
import org.mtodemo.projectionservice.document.OrderProjectionDocument;
import org.mtodemo.projectionservice.event.OrderItemInfo;
import org.mtodemo.projectionservice.event.OrderPlacedEvent;

@Mapper(componentModel = "spring")
public interface OrderProjectionMapper {

    @Mapping(source = "orderId", target = "id")
    @Mapping(target = "status", constant = "PENDING")
    OrderProjectionDocument toDocument(OrderPlacedEvent event);

    OrderItemDoc toItemDoc(OrderItemInfo item);
}
