package dev.garlandframework.demo.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import dev.garlandframework.demo.userservice.dto.OrderDto;
import dev.garlandframework.demo.userservice.dto.OrderItemDto;
import dev.garlandframework.demo.userservice.dto.OrderItemRequest;
import dev.garlandframework.demo.userservice.dto.OrderRequest;
import dev.garlandframework.demo.userservice.entity.OrderEntity;
import dev.garlandframework.demo.userservice.entity.OrderItemEntity;

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
