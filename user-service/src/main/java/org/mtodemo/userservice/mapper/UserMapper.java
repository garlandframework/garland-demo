package dev.garlandframework.demo.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import dev.garlandframework.demo.userservice.dto.*;
import dev.garlandframework.demo.userservice.entity.AddressEntity;
import dev.garlandframework.demo.userservice.entity.CarEntity;
import dev.garlandframework.demo.userservice.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "id", target = "uuid")
    UserDto toDto(UserEntity entity);

    @Mapping(source = "id", target = "uuid")
    AddressDto toAddressDto(AddressEntity entity);

    @Mapping(source = "id", target = "uuid")
    CarDto toCarDto(CarEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    UserEntity toEntity(UserRequest request);

    @Mapping(target = "id", ignore = true)
    AddressEntity toAddressEntity(AddressRequest request);

    @Mapping(target = "id", ignore = true)
    CarEntity toCarEntity(CarRequest request);
}
