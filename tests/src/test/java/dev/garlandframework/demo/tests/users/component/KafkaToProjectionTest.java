package dev.garlandframework.demo.tests.users.component;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.kafka.model.KafkaMessage;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.users.document.UserProjectionDoc;
import dev.garlandframework.demo.tests.support.users.event.UserCreatedEvent;
import dev.garlandframework.demo.tests.support.users.factory.TestUserEvents;
import dev.garlandframework.demo.tests.support.users.mapper.UserTestMapper;
import org.testng.annotations.Test;

@Test(description = "Component test: UserCreated Kafka event published directly is consumed by projection-service and projected into MongoDB")
public class KafkaToProjectionTest extends BaseTest {

    @Test(description = "A UserCreated Kafka event published directly is projected into MongoDB by the projection-service")
    public void userCreatedEvent_projectedToMongo() {
        UserCreatedEvent event = TestUserEvents.defaultUserCreatedEvent();

        Pipeline.given(new KafkaMessage<>(event.userId().toString(), event))
                .then(kafkaClient.publish())
                .execute();

        UserProjectionDoc expectedDoc = UserTestMapper.INSTANCE.toProjectionDoc(event);
        Pipeline.given(expectedDoc)
                .then(mongoClient.findById())
                .execute();
    }
}
