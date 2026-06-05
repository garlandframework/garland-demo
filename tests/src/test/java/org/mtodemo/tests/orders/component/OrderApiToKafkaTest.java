package org.mtodemo.tests.orders.component;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.orders.dto.OrderDto;
import org.mtodemo.tests.support.orders.event.OrderPlacedEvent;
import org.mtodemo.tests.support.orders.factory.TestOrderRequests;
import org.mtodemo.tests.support.orders.factory.TestOrders;
import org.mtodemo.tests.support.orders.mapper.OrderTestMapper;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.testng.annotations.Test;

@Test(description = "Component test: placing an order via HTTP persists it in Postgres and publishes a matching OrderPlaced event to Kafka")
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
                        OrderTestMapper.toEntity().andThen(postgresClient.findById()),
                        OrderTestMapper.toPlacedEvent().andThen(orderKafkaClient.consumeMatching(OrderPlacedEvent.class))
                ))
                .execute();
    }
}
