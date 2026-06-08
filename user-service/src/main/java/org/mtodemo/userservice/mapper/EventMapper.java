package dev.garlandframework.demo.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import dev.garlandframework.demo.userservice.entity.AddressEntity;
import dev.garlandframework.demo.userservice.entity.CarEntity;
import dev.garlandframework.demo.userservice.entity.UserEntity;
import dev.garlandframework.demo.userservice.event.AddressInfo;
import dev.garlandframework.demo.userservice.event.UserCreatedEvent;
import dev.garlandframework.demo.userservice.event.UserUpdatedEvent;
import dev.garlandframework.demo.userservice.event.VehicleInfo;

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
