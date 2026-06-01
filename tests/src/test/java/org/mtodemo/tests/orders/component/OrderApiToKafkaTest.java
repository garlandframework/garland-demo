package org.mtodemo.tests.orders.component;

import org.modulartestorchestrator.base.Pipeline;
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
    public void placeOrder_persistedInDb_andPublishesKafkaEvent() throws Exception {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        Pipeline.given(TestOrderRequests.placeOrder(TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(OrderTestMapper.toEntity())
                .then(dbClient.findById())
                .then(OrderTestMapper.entityToPlacedEvent())
                .then(orderKafkaClient.consumeMatching(OrderPlacedEvent.class))
                .execute();
    }
}
