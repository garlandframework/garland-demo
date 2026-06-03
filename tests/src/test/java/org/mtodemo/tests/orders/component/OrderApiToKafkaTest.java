package org.mtodemo.tests.orders.component;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.dto.OrderDto;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.event.OrderPlacedEvent;
import org.mtodemo.tests.factory.TestOrderRequests;
import org.mtodemo.tests.factory.TestOrders;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.OrderTestMapper;
import org.testng.annotations.Test;

public class OrderApiToKafkaTest extends BaseTest {

    @Test(description = "Placing an order via HTTP persists it in Postgres and publishes a matching OrderPlaced event to Kafka")
    public void placeOrder_persistedInDb_andPublishesKafkaEvent() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestOrderRequests.placeOrder(TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(trackOrder())
                .then(Verify.allOf(
                        OrderTestMapper.toEntity().andThen(dbClient.findById()),
                        OrderTestMapper.toPlacedEvent().andThen(orderKafkaClient.consumeMatching(OrderPlacedEvent.class))
                ))
                .execute();
    }
}
