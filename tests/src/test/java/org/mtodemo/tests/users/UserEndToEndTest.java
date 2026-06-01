package org.mtodemo.tests.users;

import org.modulartestorchestrator.base.Pipeline;
import org.mtodemo.tests.document.UserProjectionDoc;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.entity.UserEntity;
import org.mtodemo.tests.event.UserCreatedEvent;
import org.mtodemo.tests.event.UserDeletedEvent;
import org.mtodemo.tests.event.UserUpdatedEvent;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.factory.TestUsers;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.UserTestMapper;
import org.testng.annotations.Test;

public class UserEndToEndTest extends BaseTest {

    @Test(description = "Created user is persisted in Postgres and a UserCreated event is published to Kafka")
    public void createUser_dbThenKafka() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(UserTestMapper.toEntity())
                .then(dbClient.findById())
                .then(UserTestMapper.entityToCreatedEvent())
                .then(kafkaClient.consumeMatching(UserCreatedEvent.class))
                .execute();
    }

    @Test(description = "Creating a user publishes a UserCreated Kafka event matching the response body")
    public void createUser_publishesKafkaEvent() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(UserTestMapper.toCreatedEvent())
                .then(kafkaClient.consumeMatching(UserCreatedEvent.class))
                .execute();
    }

    @Test(description = "Creating a user triggers full system flow: Postgres persistence, Kafka event, and MongoDB projection")
    public void createUser_fullSystemFlow() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(UserTestMapper.toEntity())
                .then(dbClient.findById())
                .then(UserTestMapper.entityToCreatedEvent())
                .then(kafkaClient.consumeMatching(UserCreatedEvent.class))
                .then(UserTestMapper.toProjectionDoc())
                .then(mongoClient.findById())
                .execute();
    }

    @Test(description = "Updating a user triggers full system flow: Postgres updated, UserUpdatedEvent published to Kafka, MongoDB projection updated")
    public void updateUser_fullSystemFlow() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        UserDto updatePayload = TestUsers.defaultUser();
        Pipeline.given(TestUserRequests.updateUser(created.getUuid(), updatePayload))
                .then(httpClient.makeCall(200, UserDto.class))
                .then(UserTestMapper.toEntity())
                .then(dbClient.findById())
                .then(UserTestMapper.entityToUpdatedEvent())
                .then(kafkaClient.consumeMatching(UserUpdatedEvent.class))
                .then(UserTestMapper.toUpdatedProjectionDoc())
                .then(mongoClient.findById())
                .execute();
    }

    @Test(description = "Deleting a user triggers full system flow: removed from Postgres, UserDeletedEvent published to Kafka, MongoDB projection removed")
    public void deleteUser_fullSystemFlow() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        UserEntity expectedEntity = UserTestMapper.INSTANCE.toEntity(created);
        UserProjectionDoc expectedDoc = UserTestMapper.INSTANCE.toProjectionDoc(
                UserTestMapper.INSTANCE.toCreatedEvent(created));

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(expectedEntity)
                .then(dbClient.notExistsById())
                .execute();

        Pipeline.given(new UserDeletedEvent(created.getUuid(), null))
                .then(kafkaClient.consumeMatching(UserDeletedEvent.class))
                .execute();

        Pipeline.given(expectedDoc)
                .then(mongoClient.notExistsById())
                .execute();
    }
}
