package org.mtodemo.tests.orders.component;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.kafka.model.KafkaMessage;
import org.mtodemo.tests.document.OrderProjectionDoc;
import org.mtodemo.tests.event.OrderPlacedEvent;
import org.mtodemo.tests.factory.TestEvents;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.OrderTestMapper;
import org.testng.annotations.Test;

import java.time.Duration;

public class KafkaToOrderProjectionTest extends BaseTest {

    @Test(description = "An OrderPlaced Kafka event published directly is projected into MongoDB by the projection-service")
    public void orderPlacedEvent_projectedToMongo() {
        OrderPlacedEvent event = TestEvents.defaultOrderPlacedEvent();

        Pipeline.given(new KafkaMessage<>(event.orderId().toString(), event))
                .then(orderKafkaClient.publish())
                .execute();

        OrderProjectionDoc expectedDoc = OrderTestMapper.INSTANCE.toProjectionDoc(event);
        Pipeline.given(expectedDoc)
                .then(mongoClient.findById(Duration.ofMillis(1)))
                .execute();
    }
}
