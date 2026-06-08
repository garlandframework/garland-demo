package dev.garlandframework.demo.tests.support.users.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import dev.garlandframework.base.Step;
import dev.garlandframework.demo.tests.support.users.document.AddressInfoDoc;
import dev.garlandframework.demo.tests.support.users.document.UserProjectionDoc;
import dev.garlandframework.demo.tests.support.users.document.VehicleInfoDoc;
import dev.garlandframework.demo.tests.support.users.dto.AddressDto;
import dev.garlandframework.demo.tests.support.users.dto.CarDto;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.entity.AddressEntity;
import dev.garlandframework.demo.tests.support.users.entity.CarEntity;
import dev.garlandframework.demo.tests.support.users.entity.UserEntity;
import dev.garlandframework.demo.tests.support.users.event.AddressInfo;
import dev.garlandframework.demo.tests.support.users.event.UserCreatedEvent;
import dev.garlandframework.demo.tests.support.users.event.UserDeletedEvent;
import dev.garlandframework.demo.tests.support.users.event.UserUpdatedEvent;
import dev.garlandframework.demo.tests.support.users.event.VehicleInfo;

@Mapper
public interface UserTestMapper {

    UserTestMapper INSTANCE = Mappers.getMapper(UserTestMapper.class);

    // --- Entity mappings ---

    @Mapping(source = "uuid", target = "id")
    UserEntity toEntity(UserDto dto);

    @Mapping(source = "uuid", target = "id")
    AddressEntity toEntity(AddressDto dto);

    @Mapping(source = "uuid", target = "id")
    @Mapping(target = "userId", ignore = true)
    CarEntity toEntity(CarDto dto);

    // --- Event mappings ---

    @Mapping(source = "uuid", target = "userId")
    @Mapping(target = "fullName", expression = "java(dto.getName() + \" \" + dto.getSurname())")
    @Mapping(source = "address", target = "addressInfo")
    @Mapping(source = "cars", target = "vehicleList")
    @Mapping(target = "eventTimestamp", ignore = true)
    @Mapping(target = "sourceSystem", constant = "user-service")
    UserCreatedEvent toCreatedEvent(UserDto dto);

    @Mapping(source = "uuid", target = "addressId")
    @Mapping(source = "street", target = "streetName")
    @Mapping(source = "city", target = "cityName")
    @Mapping(source = "country", target = "countryCode")
    @Mapping(source = "zipCode", target = "postalCode")
    AddressInfo toAddressInfo(AddressDto dto);

    @Mapping(source = "uuid", target = "vehicleId")
    @Mapping(source = "plateNumber", target = "licensePlate")
    @Mapping(source = "manufacturer", target = "make")
    @Mapping(source = "model", target = "vehicleModel")
    VehicleInfo toVehicleInfo(CarDto dto);

    @Mapping(source = "id", target = "userId")
    @Mapping(target = "fullName", expression = "java(entity.getName() + \" \" + entity.getSurname())")
    @Mapping(source = "address", target = "addressInfo")
    @Mapping(source = "cars", target = "vehicleList")
    @Mapping(target = "eventTimestamp", ignore = true)
    @Mapping(target = "sourceSystem", constant = "user-service")
    UserCreatedEvent entityToCreatedEvent(UserEntity entity);

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

    // --- Updated event mappings ---

    @Mapping(source = "uuid", target = "userId")
    @Mapping(target = "fullName", expression = "java(dto.getName() + \" \" + dto.getSurname())")
    @Mapping(source = "address", target = "addressInfo")
    @Mapping(source = "cars", target = "vehicleList")
    @Mapping(target = "eventTimestamp", ignore = true)
    @Mapping(target = "sourceSystem", constant = "user-service")
    UserUpdatedEvent toUpdatedEvent(UserDto dto);

    @Mapping(source = "id", target = "userId")
    @Mapping(target = "fullName", expression = "java(entity.getName() + \" \" + entity.getSurname())")
    @Mapping(source = "address", target = "addressInfo")
    @Mapping(source = "cars", target = "vehicleList")
    @Mapping(target = "eventTimestamp", ignore = true)
    @Mapping(target = "sourceSystem", constant = "user-service")
    UserUpdatedEvent entityToUpdatedEvent(UserEntity entity);

    // --- Deleted event mappings ---

    @Mapping(source = "uuid", target = "userId")
    @Mapping(target = "eventTimestamp", ignore = true)
    UserDeletedEvent toDeletedEvent(UserDto dto);

    // --- Projection document mappings ---

    @Mapping(source = "userId", target = "id")
    @Mapping(target = "eventTimestamp", ignore = true)
    UserProjectionDoc toProjectionDoc(UserCreatedEvent event);

    @Mapping(source = "userId", target = "id")
    @Mapping(target = "eventTimestamp", ignore = true)
    UserProjectionDoc toProjectionDoc(UserUpdatedEvent event);

    AddressInfoDoc toAddressInfoDoc(AddressInfo addressInfo);

    VehicleInfoDoc toVehicleInfoDoc(VehicleInfo vehicleInfo);

    // --- Pipeline bridges ---

    static Step<UserDto, UserEntity> toEntity() {
        return Step.lift(INSTANCE::toEntity);
    }

    static Step<UserDto, UserCreatedEvent> toCreatedEvent() {
        return Step.lift(INSTANCE::toCreatedEvent);
    }

    static Step<UserEntity, UserCreatedEvent> entityToCreatedEvent() {
        return Step.lift(INSTANCE::entityToCreatedEvent);
    }

    static Step<UserCreatedEvent, UserProjectionDoc> toProjectionDoc() {
        return Step.lift(INSTANCE::toProjectionDoc);
    }

    static Step<UserDto, UserUpdatedEvent> toUpdatedEvent() {
        return Step.lift(INSTANCE::toUpdatedEvent);
    }

    static Step<UserEntity, UserUpdatedEvent> entityToUpdatedEvent() {
        return Step.lift(INSTANCE::entityToUpdatedEvent);
    }

    static Step<UserUpdatedEvent, UserProjectionDoc> toUpdatedProjectionDoc() {
        return Step.lift(event -> INSTANCE.toProjectionDoc(event));
    }

    static Step<UserDto, UserDeletedEvent> toDeletedEvent() {
        return Step.lift(INSTANCE::toDeletedEvent);
    }

    static Step<UserDto, UserProjectionDoc> dtoToCreatedProjectionDoc() {
        return Step.lift(dto -> INSTANCE.toProjectionDoc(INSTANCE.toCreatedEvent(dto)));
    }

    static Step<UserDto, UserProjectionDoc> dtoToUpdatedProjectionDoc() {
        return Step.lift(dto -> INSTANCE.toProjectionDoc(INSTANCE.toUpdatedEvent(dto)));
    }
}
