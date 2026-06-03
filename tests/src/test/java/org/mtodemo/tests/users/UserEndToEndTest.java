package org.mtodemo.tests.users;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.event.UserCreatedEvent;
import org.mtodemo.tests.support.users.event.UserDeletedEvent;
import org.mtodemo.tests.support.users.event.UserUpdatedEvent;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.mtodemo.tests.support.users.factory.TestUsers;
import org.mtodemo.tests.support.users.mapper.UserTestMapper;
import org.testng.annotations.Test;

public class UserEndToEndTest extends BaseTest {

    @Test(description = "Created user is persisted in Postgres and a UserCreated event is published to Kafka")
    public void createUser_dbThenKafka() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(Verify.allOf(
                        UserTestMapper.toEntity().andThen(dbClient.findById()),
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

    @Test(description = "Creating a user triggers full system flow: Postgres persistence, Kafka event, and MongoDB projection")
    public void createUser_fullSystemFlow() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(Verify.allOf(
                        UserTestMapper.toEntity().andThen(dbClient.findById()),
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
                        UserTestMapper.toEntity().andThen(dbClient.findById()),
                        UserTestMapper.toUpdatedEvent().andThen(kafkaClient.consumeMatching(UserUpdatedEvent.class)),
                        UserTestMapper.dtoToUpdatedProjectionDoc().andThen(mongoClient.findById())
                ))
                .execute();
    }

    @Test(description = "Deleting a user triggers full system flow: removed from Postgres, UserDeletedEvent published to Kafka, MongoDB projection removed")
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
                        UserTestMapper.toEntity().andThen(dbClient.notExistsById()),
                        UserTestMapper.toDeletedEvent().andThen(kafkaClient.consumeMatching(UserDeletedEvent.class)),
                        UserTestMapper.dtoToCreatedProjectionDoc().andThen(mongoClient.notExistsById())
                ))
                .execute();
    }
}
