package org.mtodemo.tests.orders.component;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.kafka.model.KafkaMessage;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.orders.document.OrderProjectionDoc;
import org.mtodemo.tests.support.orders.event.OrderPlacedEvent;
import org.mtodemo.tests.support.orders.factory.TestOrderEvents;
import org.mtodemo.tests.support.orders.mapper.OrderTestMapper;
import org.testng.annotations.Test;

public class KafkaToOrderProjectionTest extends BaseTest {

    @Test(description = "An OrderPlaced Kafka event published directly is projected into MongoDB by the projection-service")
    public void orderPlacedEvent_projectedToMongo() {
        OrderPlacedEvent event = TestOrderEvents.defaultOrderPlacedEvent();

        Pipeline.given(new KafkaMessage<>(event.orderId().toString(), event))
                .then(orderKafkaClient.publish())
                .execute();

        OrderProjectionDoc expectedDoc = OrderTestMapper.INSTANCE.toProjectionDoc(event);
        Pipeline.given(expectedDoc)
                .then(mongoClient.findById())
                .execute();
    }
}
