package org.mtodemo.tests.users.endpoint;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.modulartestorchestrator.http.model.HttpCallRequest;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.dto.ValidationErrorDto;
import org.mtodemo.tests.factory.TestAddresses;
import org.mtodemo.tests.factory.TestCars;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.factory.TestUsers;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.UserTestMapper;
import org.testng.annotations.Test;

import java.util.List;

public class CreateUserApiTest extends BaseTest {

    @Test(description = "POST /api/users with valid data returns 201 and user is persisted in Postgres")
    public void createUser_persistedInDb() throws Exception {
        HttpCallRequest<UserDto> request = TestUserRequests.createUser();
        Pipeline.given(request)
                .then(httpClient.makeCall(201, UserDto.class))
                .then(Verify.matching(request.dto()))
                .then(UserTestMapper.toEntity())
                .then(dbClient.findById())
                .execute();
    }

    // --- required fields ---

    @Test(description = "Blank name is rejected with 400 and error pointing to 'name' field")
    public void createUser_blankName_returns400() throws Exception {
        Pipeline.given(TestUserRequests.createUser(TestUsers.builder().name("").build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("name")))
                .execute();
    }

    @Test(description = "Null name is rejected with 400 and error pointing to 'name' field")
    public void createUser_nullName_returns400() throws Exception {
        Pipeline.given(TestUserRequests.createUser(TestUsers.builder().name(null).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("name")))
                .execute();
    }

    @Test(description = "Blank surname is rejected with 400 and error pointing to 'surname' field")
    public void createUser_blankSurname_returns400() throws Exception {
        Pipeline.given(TestUserRequests.createUser(TestUsers.builder().surname("").build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("surname")))
                .execute();
    }

    @Test(description = "Null surname is rejected with 400 and error pointing to 'surname' field")
    public void createUser_nullSurname_returns400() throws Exception {
        Pipeline.given(TestUserRequests.createUser(TestUsers.builder().surname(null).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("surname")))
                .execute();
    }

    @Test(description = "Blank address street is rejected with 400 and error pointing to 'address.street' field")
    public void createUser_blankAddressStreet_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().street("").build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.street")))
                .execute();
    }

    @Test(description = "Null address street is rejected with 400 and error pointing to 'address.street' field")
    public void createUser_nullAddressStreet_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().street(null).build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.street")))
                .execute();
    }

    @Test(description = "Blank address city is rejected with 400 and error pointing to 'address.city' field")
    public void createUser_blankAddressCity_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().city("").build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.city")))
                .execute();
    }

    @Test(description = "Null address city is rejected with 400 and error pointing to 'address.city' field")
    public void createUser_nullAddressCity_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().city(null).build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.city")))
                .execute();
    }

    @Test(description = "Blank address country is rejected with 400 and error pointing to 'address.country' field")
    public void createUser_blankAddressCountry_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().country("").build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.country")))
                .execute();
    }

    @Test(description = "Null address country is rejected with 400 and error pointing to 'address.country' field")
    public void createUser_nullAddressCountry_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().country(null).build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.country")))
                .execute();
    }

    @Test(description = "Blank address zip code is rejected with 400 and error pointing to 'address.zipCode' field")
    public void createUser_blankAddressZipCode_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().zipCode("").build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.zipCode")))
                .execute();
    }

    @Test(description = "Null address zip code is rejected with 400 and error pointing to 'address.zipCode' field")
    public void createUser_nullAddressZipCode_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().zipCode(null).build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.zipCode")))
                .execute();
    }

    @Test(description = "Blank car plate number is rejected with 400 and error pointing to 'cars[0].plateNumber' field")
    public void createUser_blankCarPlateNumber_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .cars(List.of(TestCars.builder().plateNumber("").build()))
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].plateNumber")))
                .execute();
    }

    @Test(description = "Null car plate number is rejected with 400 and error pointing to 'cars[0].plateNumber' field")
    public void createUser_nullCarPlateNumber_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .cars(List.of(TestCars.builder().plateNumber(null).build()))
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].plateNumber")))
                .execute();
    }

    @Test(description = "Blank car manufacturer is rejected with 400 and error pointing to 'cars[0].manufacturer' field")
    public void createUser_blankCarManufacturer_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .cars(List.of(TestCars.builder().manufacturer("").build()))
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].manufacturer")))
                .execute();
    }

    @Test(description = "Null car manufacturer is rejected with 400 and error pointing to 'cars[0].manufacturer' field")
    public void createUser_nullCarManufacturer_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .cars(List.of(TestCars.builder().manufacturer(null).build()))
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].manufacturer")))
                .execute();
    }

    @Test(description = "Blank car model is rejected with 400 and error pointing to 'cars[0].model' field")
    public void createUser_blankCarModel_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .cars(List.of(TestCars.builder().model("").build()))
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].model")))
                .execute();
    }

    @Test(description = "Null car model is rejected with 400 and error pointing to 'cars[0].model' field")
    public void createUser_nullCarModel_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .cars(List.of(TestCars.builder().model(null).build()))
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].model")))
                .execute();
    }

    // --- size limits ---

    @Test(description = "Name exceeding 100 characters is rejected with 400 and error pointing to 'name' field")
    public void createUser_nameTooLong_returns400() throws Exception {
        Pipeline.given(TestUserRequests.createUser(TestUsers.builder().name("a".repeat(101)).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("name")))
                .execute();
    }

    @Test(description = "Surname exceeding 100 characters is rejected with 400 and error pointing to 'surname' field")
    public void createUser_surnameTooLong_returns400() throws Exception {
        Pipeline.given(TestUserRequests.createUser(TestUsers.builder().surname("a".repeat(101)).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("surname")))
                .execute();
    }

    @Test(description = "Address street exceeding 255 characters is rejected with 400 and error pointing to 'address.street' field")
    public void createUser_addressStreetTooLong_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().street("a".repeat(256)).build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.street")))
                .execute();
    }

    @Test(description = "Address city exceeding 100 characters is rejected with 400 and error pointing to 'address.city' field")
    public void createUser_addressCityTooLong_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().city("a".repeat(101)).build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.city")))
                .execute();
    }

    @Test(description = "Address country exceeding 100 characters is rejected with 400 and error pointing to 'address.country' field")
    public void createUser_addressCountryTooLong_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().country("a".repeat(101)).build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.country")))
                .execute();
    }

    @Test(description = "Address zip code exceeding 20 characters is rejected with 400 and error pointing to 'address.zipCode' field")
    public void createUser_addressZipCodeTooLong_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .address(TestAddresses.builder().zipCode("a".repeat(21)).build())
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.zipCode")))
                .execute();
    }

    @Test(description = "Car plate number exceeding 20 characters is rejected with 400 and error pointing to 'cars[0].plateNumber' field")
    public void createUser_carPlateNumberTooLong_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .cars(List.of(TestCars.builder().plateNumber("a".repeat(21)).build()))
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].plateNumber")))
                .execute();
    }

    @Test(description = "Car manufacturer exceeding 100 characters is rejected with 400 and error pointing to 'cars[0].manufacturer' field")
    public void createUser_carManufacturerTooLong_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .cars(List.of(TestCars.builder().manufacturer("a".repeat(101)).build()))
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].manufacturer")))
                .execute();
    }

    @Test(description = "Car model exceeding 100 characters is rejected with 400 and error pointing to 'cars[0].model' field")
    public void createUser_carModelTooLong_returns400() throws Exception {
        UserDto user = TestUsers.builder()
                .cars(List.of(TestCars.builder().model("a".repeat(101)).build()))
                .build();
        Pipeline.given(TestUserRequests.createUser(user))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].model")))
                .execute();
    }

}
