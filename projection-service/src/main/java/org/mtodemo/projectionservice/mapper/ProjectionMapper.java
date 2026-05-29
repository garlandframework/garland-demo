package org.mtodemo.projectionservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mtodemo.projectionservice.document.AddressInfoDoc;
import org.mtodemo.projectionservice.document.UserProjectionDocument;
import org.mtodemo.projectionservice.document.VehicleInfoDoc;
import org.mtodemo.projectionservice.event.AddressInfo;
import org.mtodemo.projectionservice.event.UserCreatedEvent;
import org.mtodemo.projectionservice.event.UserUpdatedEvent;
import org.mtodemo.projectionservice.event.VehicleInfo;

@Mapper(componentModel = "spring")
public interface ProjectionMapper {

    @Mapping(source = "userId", target = "id")
    UserProjectionDocument toDocument(UserCreatedEvent event);

    @Mapping(source = "userId", target = "id")
    UserProjectionDocument toDocument(UserUpdatedEvent event);

    AddressInfoDoc toAddressInfoDoc(AddressInfo addressInfo);

    VehicleInfoDoc toVehicleInfoDoc(VehicleInfo vehicleInfo);
}
