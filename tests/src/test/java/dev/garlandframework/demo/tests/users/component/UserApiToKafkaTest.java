package dev.garlandframework.demo.tests.users.component;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.event.UserCreatedEvent;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
import dev.garlandframework.demo.tests.support.users.mapper.UserTestMapper;
import org.testng.annotations.Test;

@Test(description = "Component test: creating a user via HTTP persists it in Postgres and publishes a matching UserCreated event to Kafka")
public class UserApiToKafkaTest extends BaseTest {

    @Test(description = "Creating a user via HTTP persists it in Postgres and publishes a matching UserCreated event to Kafka")
    public void createUser_persistedInDb_andPublishesKafkaEvent() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(Verify.allOf(
                        UserTestMapper.toEntity().andThen(postgresClient.findById()),
                        UserTestMapper.toCreatedEvent().andThen(kafkaClient.consumeMatching(UserCreatedEvent.class))
                ))
                .execute();
    }
}
