package org.mtodemo.tests.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.modulartestorchestrator.base.StepFunction;
import org.mtodemo.tests.document.AddressInfoDoc;
import org.mtodemo.tests.document.UserProjectionDoc;
import org.mtodemo.tests.document.VehicleInfoDoc;
import org.mtodemo.tests.dto.AddressDto;
import org.mtodemo.tests.dto.CarDto;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.entity.AddressEntity;
import org.mtodemo.tests.entity.CarEntity;
import org.mtodemo.tests.entity.UserEntity;
import org.mtodemo.tests.event.AddressInfo;
import org.mtodemo.tests.event.UserCreatedEvent;
import org.mtodemo.tests.event.UserDeletedEvent;
import org.mtodemo.tests.event.UserUpdatedEvent;
import org.mtodemo.tests.event.VehicleInfo;

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

    static StepFunction<UserDto, UserEntity> toEntity() {
        return StepFunction.lift(INSTANCE::toEntity);
    }

    static StepFunction<UserDto, UserCreatedEvent> toCreatedEvent() {
        return StepFunction.lift(INSTANCE::toCreatedEvent);
    }

    static StepFunction<UserEntity, UserCreatedEvent> entityToCreatedEvent() {
        return StepFunction.lift(INSTANCE::entityToCreatedEvent);
    }

    static StepFunction<UserCreatedEvent, UserProjectionDoc> toProjectionDoc() {
        return StepFunction.lift(INSTANCE::toProjectionDoc);
    }

    static StepFunction<UserDto, UserUpdatedEvent> toUpdatedEvent() {
        return StepFunction.lift(INSTANCE::toUpdatedEvent);
    }

    static StepFunction<UserEntity, UserUpdatedEvent> entityToUpdatedEvent() {
        return StepFunction.lift(INSTANCE::entityToUpdatedEvent);
    }

    static StepFunction<UserUpdatedEvent, UserProjectionDoc> toUpdatedProjectionDoc() {
        return StepFunction.lift(event -> INSTANCE.toProjectionDoc(event));
    }
}
