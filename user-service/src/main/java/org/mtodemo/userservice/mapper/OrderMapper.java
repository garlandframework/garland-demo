package org.mtodemo.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mtodemo.userservice.dto.OrderDto;
import org.mtodemo.userservice.dto.OrderItemDto;
import org.mtodemo.userservice.dto.OrderItemRequest;
import org.mtodemo.userservice.dto.OrderRequest;
import org.mtodemo.userservice.entity.OrderEntity;
import org.mtodemo.userservice.entity.OrderItemEntity;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "id", target = "uuid")
    OrderDto toDto(OrderEntity entity);

    @Mapping(source = "id", target = "uuid")
    OrderItemDto toItemDto(OrderItemEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OrderEntity toEntity(OrderRequest request);

    @Mapping(target = "id", ignore = true)
    OrderItemEntity toItemEntity(OrderItemRequest request);
}
