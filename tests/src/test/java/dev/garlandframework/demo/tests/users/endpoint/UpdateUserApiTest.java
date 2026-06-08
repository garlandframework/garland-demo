package dev.garlandframework.demo.tests.users.endpoint;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.common.dto.ErrorDto;
import dev.garlandframework.demo.tests.support.common.dto.ValidationErrorDto;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.factory.TestAddresses;
import dev.garlandframework.demo.tests.support.users.factory.TestCars;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
import dev.garlandframework.demo.tests.support.users.factory.TestUsers;
import dev.garlandframework.demo.tests.support.users.mapper.UserTestMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

@Test(description = "Endpoint tests for PUT /api/users/{id}: successful update, required-fields-only update, not found, and validation rejections")
public class UpdateUserApiTest extends BaseTest {

    private UserDto created;

    @BeforeMethod
    public void createUser() {
        created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    @Test(description = "PUT /api/users/{id} with valid data returns 200 and changes are persisted in Postgres")
    public void updateUser_persistedInDb() {
        UserDto updatePayload = TestUsers.defaultUser();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(Verify.matching(updatePayload))
                .then(UserTestMapper.toEntity())
                .then(postgresClient.findByFields())
                .execute();
    }

    @Test(description = "PUT /api/users/{id} with required fields only returns 200 and clears optional fields in Postgres")
    public void updateUser_requiredFieldsOnly_clearsOptionalFields() {
        UserDto updatePayload = TestUsers.requiredFieldsOnlyUser();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(Verify.matching(updatePayload))
                .then(UserTestMapper.toEntity())
                .then(postgresClient.findByFields())
                .execute();
    }

    // --- not found ---

    @Test(description = "PUT /api/users/{id} with non-existent id returns 404 with error body")
    public void updateUser_nonExistentId_returns404() {
        Pipeline.given(TestUserRequests.updateUser(UUID.randomUUID(), TestUsers.defaultUser()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .then(Verify.matching(ErrorDto.withStatus(404)))
                .execute();
    }

    // --- required fields ---

    @Test(description = "Blank name is rejected with 400 and error pointing to 'name' field")
    public void updateUser_blankName_returns400() {
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), TestUsers.builder().name("").build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("name")))
                .execute();
    }

    @Test(description = "Null name is rejected with 400 and error pointing to 'name' field")
    public void updateUser_nullName_returns400() {
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), TestUsers.builder().name(null).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("name")))
                .execute();
    }

    @Test(description = "Blank surname is rejected with 400 and error pointing to 'surname' field")
    public void updateUser_blankSurname_returns400() {
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), TestUsers.builder().surname("").build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("surname")))
                .execute();
    }

    @Test(description = "Null surname is rejected with 400 and error pointing to 'surname' field")
    public void updateUser_nullSurname_returns400() {
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), TestUsers.builder().surname(null).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("surname")))
                .execute();
    }

    @Test(description = "Blank address street is rejected with 400 and error pointing to 'address.street' field")
    public void updateUser_blankAddressStreet_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().street("").build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.street")))
                .execute();
    }

    @Test(description = "Null address street is rejected with 400 and error pointing to 'address.street' field")
    public void updateUser_nullAddressStreet_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().street(null).build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.street")))
                .execute();
    }

    @Test(description = "Blank address city is rejected with 400 and error pointing to 'address.city' field")
    public void updateUser_blankAddressCity_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().city("").build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.city")))
                .execute();
    }

    @Test(description = "Null address city is rejected with 400 and error pointing to 'address.city' field")
    public void updateUser_nullAddressCity_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().city(null).build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.city")))
                .execute();
    }

    @Test(description = "Blank address country is rejected with 400 and error pointing to 'address.country' field")
    public void updateUser_blankAddressCountry_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().country("").build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.country")))
                .execute();
    }

    @Test(description = "Null address country is rejected with 400 and error pointing to 'address.country' field")
    public void updateUser_nullAddressCountry_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().country(null).build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.country")))
                .execute();
    }

    @Test(description = "Blank address zip code is rejected with 400 and error pointing to 'address.zipCode' field")
    public void updateUser_blankAddressZipCode_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().zipCode("").build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.zipCode")))
                .execute();
    }

    @Test(description = "Null address zip code is rejected with 400 and error pointing to 'address.zipCode' field")
    public void updateUser_nullAddressZipCode_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().zipCode(null).build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.zipCode")))
                .execute();
    }

    @Test(description = "Blank car plate number is rejected with 400 and error pointing to 'cars[0].plateNumber' field")
    public void updateUser_blankCarPlateNumber_returns400() {
        UserDto update = TestUsers.builder()
                .cars(List.of(TestCars.builder().plateNumber("").build()))
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].plateNumber")))
                .execute();
    }

    @Test(description = "Null car plate number is rejected with 400 and error pointing to 'cars[0].plateNumber' field")
    public void updateUser_nullCarPlateNumber_returns400() {
        UserDto update = TestUsers.builder()
                .cars(List.of(TestCars.builder().plateNumber(null).build()))
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].plateNumber")))
                .execute();
    }

    @Test(description = "Blank car manufacturer is rejected with 400 and error pointing to 'cars[0].manufacturer' field")
    public void updateUser_blankCarManufacturer_returns400() {
        UserDto update = TestUsers.builder()
                .cars(List.of(TestCars.builder().manufacturer("").build()))
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].manufacturer")))
                .execute();
    }

    @Test(description = "Null car manufacturer is rejected with 400 and error pointing to 'cars[0].manufacturer' field")
    public void updateUser_nullCarManufacturer_returns400() {
        UserDto update = TestUsers.builder()
                .cars(List.of(TestCars.builder().manufacturer(null).build()))
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].manufacturer")))
                .execute();
    }

    @Test(description = "Blank car model is rejected with 400 and error pointing to 'cars[0].model' field")
    public void updateUser_blankCarModel_returns400() {
        UserDto update = TestUsers.builder()
                .cars(List.of(TestCars.builder().model("").build()))
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].model")))
                .execute();
    }

    @Test(description = "Null car model is rejected with 400 and error pointing to 'cars[0].model' field")
    public void updateUser_nullCarModel_returns400() {
        UserDto update = TestUsers.builder()
                .cars(List.of(TestCars.builder().model(null).build()))
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].model")))
                .execute();
    }

    // --- size limits ---

    @Test(description = "Name exceeding 100 characters is rejected with 400 and error pointing to 'name' field")
    public void updateUser_nameTooLong_returns400() {
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), TestUsers.builder().name("a".repeat(101)).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("name")))
                .execute();
    }

    @Test(description = "Surname exceeding 100 characters is rejected with 400 and error pointing to 'surname' field")
    public void updateUser_surnameTooLong_returns400() {
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), TestUsers.builder().surname("a".repeat(101)).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("surname")))
                .execute();
    }

    @Test(description = "Address street exceeding 255 characters is rejected with 400 and error pointing to 'address.street' field")
    public void updateUser_addressStreetTooLong_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().street("a".repeat(256)).build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.street")))
                .execute();
    }

    @Test(description = "Address city exceeding 100 characters is rejected with 400 and error pointing to 'address.city' field")
    public void updateUser_addressCityTooLong_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().city("a".repeat(101)).build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.city")))
                .execute();
    }

    @Test(description = "Address country exceeding 100 characters is rejected with 400 and error pointing to 'address.country' field")
    public void updateUser_addressCountryTooLong_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().country("a".repeat(101)).build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.country")))
                .execute();
    }

    @Test(description = "Address zip code exceeding 20 characters is rejected with 400 and error pointing to 'address.zipCode' field")
    public void updateUser_addressZipCodeTooLong_returns400() {
        UserDto update = TestUsers.builder()
                .address(TestAddresses.builder().zipCode("a".repeat(21)).build())
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("address.zipCode")))
                .execute();
    }

    @Test(description = "Car plate number exceeding 20 characters is rejected with 400 and error pointing to 'cars[0].plateNumber' field")
    public void updateUser_carPlateNumberTooLong_returns400() {
        UserDto update = TestUsers.builder()
                .cars(List.of(TestCars.builder().plateNumber("a".repeat(21)).build()))
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].plateNumber")))
                .execute();
    }

    @Test(description = "Car manufacturer exceeding 100 characters is rejected with 400 and error pointing to 'cars[0].manufacturer' field")
    public void updateUser_carManufacturerTooLong_returns400() {
        UserDto update = TestUsers.builder()
                .cars(List.of(TestCars.builder().manufacturer("a".repeat(101)).build()))
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].manufacturer")))
                .execute();
    }

    @Test(description = "Car model exceeding 100 characters is rejected with 400 and error pointing to 'cars[0].model' field")
    public void updateUser_carModelTooLong_returns400() {
        UserDto update = TestUsers.builder()
                .cars(List.of(TestCars.builder().model("a".repeat(101)).build()))
                .build();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), update))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("cars[0].model")))
                .execute();
    }
}
