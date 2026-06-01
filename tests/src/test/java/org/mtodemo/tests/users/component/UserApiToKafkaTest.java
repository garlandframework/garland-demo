package org.mtodemo.tests.users.component;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.event.UserCreatedEvent;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.UserTestMapper;
import org.testng.annotations.Test;

public class UserApiToKafkaTest extends BaseTest {

    @Test(description = "Creating a user via HTTP persists it in Postgres and publishes a matching UserCreated event to Kafka")
    public void createUser_persistedInDb_andPublishesKafkaEvent() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(Verify.allOf(
                        UserTestMapper.toEntity().andThen(dbClient.findById()),
                        UserTestMapper.toCreatedEvent().andThen(kafkaClient.consumeMatching(UserCreatedEvent.class))
                ))
                .execute();
    }
}
