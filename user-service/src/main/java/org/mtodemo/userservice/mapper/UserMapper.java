package org.mtodemo.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mtodemo.userservice.dto.*;
import org.mtodemo.userservice.entity.AddressEntity;
import org.mtodemo.userservice.entity.CarEntity;
import org.mtodemo.userservice.entity.UserEntity;

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
