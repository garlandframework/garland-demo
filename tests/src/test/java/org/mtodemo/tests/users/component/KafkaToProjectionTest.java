package org.mtodemo.tests.users.component;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.kafka.model.KafkaMessage;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.users.document.UserProjectionDoc;
import org.mtodemo.tests.support.users.event.UserCreatedEvent;
import org.mtodemo.tests.support.users.factory.TestUserEvents;
import org.mtodemo.tests.support.users.mapper.UserTestMapper;
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
