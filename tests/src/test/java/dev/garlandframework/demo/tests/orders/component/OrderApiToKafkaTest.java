package dev.garlandframework.demo.tests.orders.component;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.orders.dto.OrderDto;
import dev.garlandframework.demo.tests.support.orders.event.OrderPlacedEvent;
import dev.garlandframework.demo.tests.support.orders.factory.TestOrderRequests;
import dev.garlandframework.demo.tests.support.orders.factory.TestOrders;
import dev.garlandframework.demo.tests.support.orders.mapper.OrderTestMapper;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
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
