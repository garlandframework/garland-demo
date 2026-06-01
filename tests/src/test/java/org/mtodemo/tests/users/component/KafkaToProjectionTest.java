package org.mtodemo.tests.users.component;

import org.modulartestorchestrator.base.Pipeline;
import org.mtodemo.tests.document.UserProjectionDoc;
import org.mtodemo.tests.event.UserCreatedEvent;
import org.mtodemo.tests.factory.TestEvents;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.UserTestMapper;
import org.modulartestorchestrator.kafka.model.KafkaMessage;
import org.testng.annotations.Test;

public class KafkaToProjectionTest extends BaseTest {

    @Test(description = "A UserCreated Kafka event published directly is projected into MongoDB by the projection-service")
    public void userCreatedEvent_projectedToMongo() {
        UserCreatedEvent event = TestEvents.defaultUserCreatedEvent();

        Pipeline.given(new KafkaMessage<>(event.userId().toString(), event))
                .then(kafkaClient.publish())
                .execute();

        UserProjectionDoc expectedDoc = UserTestMapper.INSTANCE.toProjectionDoc(event);
        Pipeline.given(expectedDoc)
                .then(mongoClient.findById())
                .execute();
    }
}
