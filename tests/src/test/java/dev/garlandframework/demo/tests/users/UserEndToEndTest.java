package dev.garlandframework.demo.tests.users;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.event.UserCreatedEvent;
import dev.garlandframework.demo.tests.support.users.event.UserDeletedEvent;
import dev.garlandframework.demo.tests.support.users.event.UserUpdatedEvent;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
import dev.garlandframework.demo.tests.support.users.factory.TestUsers;
import dev.garlandframework.demo.tests.support.users.mapper.UserTestMapper;
import dev.garlandframework.http.model.HttpCallResponse;
import org.testng.annotations.Test;

import java.util.Map;

@Test(description = "End-to-end tests for user-related flows, covering the full system from API to Postgres, Kafka, and MongoDB")
public class UserEndToEndTest extends BaseTest {

    @Test(description = "Created user is persisted in Postgres and a UserCreated event is published to Kafka")
    public void createUser_dbThenKafka() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(Verify.allOf(
                        UserTestMapper.toEntity().andThen(postgresClient.findById()),
                        UserTestMapper.toCreatedEvent().andThen(kafkaClient.consumeMatching(UserCreatedEvent.class))
                ))
                .execute();
    }

    @Test(description = "Creating a user publishes a UserCreated Kafka event matching the response body")
    public void createUser_publishesKafkaEvent() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(UserTestMapper.toCreatedEvent())
                .then(kafkaClient.consumeMatching(UserCreatedEvent.class))
                .execute();
    }


    @Test(description = "Creating a user is fully verified across all systems: DTO response, Postgres, Kafka, and MongoDB — all fields checked")
    public void createUser_allFieldsVerifiedAcrossAllSystems_separateDtoCheck() {
        UserDto expected = TestUsers.defaultUser();

        Pipeline.given(TestUserRequests.createUser(expected))
                .then(httpClient.makeCall(201, UserDto.class))
                .then(Verify.matching(expected))
                .then(trackUser())
                .then(Verify.allOf(
                        UserTestMapper.toEntity().andThen(postgresClient.findByFields()),
                        UserTestMapper.toCreatedEvent().andThen(kafkaClient.consumeMatching(UserCreatedEvent.class)),
                        UserTestMapper.dtoToCreatedProjectionDoc().andThen(mongoClient.findByFields())
                ))
                .execute();
    }

    @Test(description = "Creating a user triggers full system flow: Postgres persistence, Kafka event, and MongoDB projection")
    public void createUser_fullSystemFlow() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(Verify.allOf(
                        UserTestMapper.toEntity().andThen(postgresClient.findById()),
                        UserTestMapper.toCreatedEvent().andThen(kafkaClient.consumeMatching(UserCreatedEvent.class)),
                        UserTestMapper.dtoToCreatedProjectionDoc().andThen(mongoClient.findById())
                ))
                .execute();
    }

    @Test(description = "Updating a user triggers full system flow: Postgres updated, UserUpdatedEvent published to Kafka, MongoDB projection updated")
    public void updateUser_fullSystemFlow() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserDto updatePayload = TestUsers.defaultUser();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(Verify.allOf(
                        UserTestMapper.toEntity().andThen(postgresClient.findById()),
                        UserTestMapper.toUpdatedEvent().andThen(kafkaClient.consumeMatching(UserUpdatedEvent.class)),
                        UserTestMapper.dtoToUpdatedProjectionDoc().andThen(mongoClient.findById())
                ))
                .execute();
    }

    @Test(description = "Deleting a user tri" +
            "ggers full system flow: removed from Postgres, UserDeletedEvent published to Kafka, MongoDB projection removed")
    public void deleteUser_fullSystemFlow() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(created)
                .then(Verify.allOf(
                        UserTestMapper.toEntity().andThen(postgresClient.notExistsById()),
                        UserTestMapper.toDeletedEvent().andThen(kafkaClient.consumeMatching(UserDeletedEvent.class)),
                        UserTestMapper.dtoToCreatedProjectionDoc().andThen(mongoClient.notExistsById())
                ))
                .execute();
    }
}
