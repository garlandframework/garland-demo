package dev.garlandframework.demo.projectionservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import dev.garlandframework.demo.projectionservice.document.AddressInfoDoc;
import dev.garlandframework.demo.projectionservice.document.UserProjectionDocument;
import dev.garlandframework.demo.projectionservice.document.VehicleInfoDoc;
import dev.garlandframework.demo.projectionservice.event.AddressInfo;
import dev.garlandframework.demo.projectionservice.event.UserCreatedEvent;
import dev.garlandframework.demo.projectionservice.event.UserUpdatedEvent;
import dev.garlandframework.demo.projectionservice.event.VehicleInfo;

@Mapper(componentModel = "spring")
public interface ProjectionMapper {

    @Mapping(source = "userId", target = "id")
    UserProjectionDocument toDocument(UserCreatedEvent event);

    @Mapping(source = "userId", target = "id")
    UserProjectionDocument toDocument(UserUpdatedEvent event);

    AddressInfoDoc toAddressInfoDoc(AddressInfo addressInfo);

    VehicleInfoDoc toVehicleInfoDoc(VehicleInfo vehicleInfo);
}
