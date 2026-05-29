package org.mtodemo.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mtodemo.userservice.entity.AddressEntity;
import org.mtodemo.userservice.entity.CarEntity;
import org.mtodemo.userservice.entity.UserEntity;
import org.mtodemo.userservice.event.AddressInfo;
import org.mtodemo.userservice.event.UserCreatedEvent;
import org.mtodemo.userservice.event.UserUpdatedEvent;
import org.mtodemo.userservice.event.VehicleInfo;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(source = "id", target = "userId")
    @Mapping(target = "fullName", expression = "java(entity.getName() + \" \" + entity.getSurname())")
    @Mapping(source = "address", target = "addressInfo")
    @Mapping(source = "cars", target = "vehicleList")
    @Mapping(target = "eventTimestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "sourceSystem", constant = "user-service")
    UserCreatedEvent toCreatedEvent(UserEntity entity);

    @Mapping(source = "id", target = "userId")
    @Mapping(target = "fullName", expression = "java(entity.getName() + \" \" + entity.getSurname())")
    @Mapping(source = "address", target = "addressInfo")
    @Mapping(source = "cars", target = "vehicleList")
    @Mapping(target = "eventTimestamp", expression = "java(java.time.Instant.now())")
    @Mapping(target = "sourceSystem", constant = "user-service")
    UserUpdatedEvent toUpdatedEvent(UserEntity entity);

    @Mapping(source = "id", target = "addressId")
    @Mapping(source = "street", target = "streetName")
    @Mapping(source = "city", target = "cityName")
    @Mapping(source = "country", target = "countryCode")
    @Mapping(source = "zipCode", target = "postalCode")
    AddressInfo toAddressInfo(AddressEntity entity);

    @Mapping(source = "id", target = "vehicleId")
    @Mapping(source = "plateNumber", target = "licensePlate")
    @Mapping(source = "manufacturer", target = "make")
    @Mapping(source = "model", target = "vehicleModel")
    VehicleInfo toVehicleInfo(CarEntity entity);
}
