package dev.garlandframework.demo.tests.orders.component;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.kafka.model.KafkaMessage;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.orders.document.OrderProjectionDoc;
import dev.garlandframework.demo.tests.support.orders.event.OrderPlacedEvent;
import dev.garlandframework.demo.tests.support.orders.factory.TestOrderEvents;
import dev.garlandframework.demo.tests.support.orders.mapper.OrderTestMapper;
import org.testng.annotations.Test;

@Test(description = "Component test: OrderPlaced Kafka event published directly is consumed by projection-service and projected into MongoDB")
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
