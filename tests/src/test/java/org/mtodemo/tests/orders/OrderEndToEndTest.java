package org.mtodemo.tests.orders;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.orders.dto.OrderDto;
import org.mtodemo.tests.support.orders.event.OrderCancelledEvent;
import org.mtodemo.tests.support.orders.event.OrderPlacedEvent;
import org.mtodemo.tests.support.orders.factory.TestOrderRequests;
import org.mtodemo.tests.support.orders.factory.TestOrders;
import org.mtodemo.tests.support.orders.mapper.OrderTestMapper;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.testng.annotations.Test;

public class OrderEndToEndTest extends BaseTest {

    @Test(description = "Placing an order triggers full system flow: persisted in Postgres, OrderPlaced event published to Kafka, projected into MongoDB")
    public void placeOrder_fullSystemFlow() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Pipeline.given(TestOrderRequests.placeOrder(TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(trackOrder())
                .then(Verify.allOf(
                        OrderTestMapper.toEntity().andThen(dbClient.findById()),
                        OrderTestMapper.toPlacedEvent().andThen(orderKafkaClient.consumeMatching(OrderPlacedEvent.class)),
                        OrderTestMapper.toPlacedEvent().andThen(OrderTestMapper.toProjectionDoc()).andThen(mongoClient.findById())
                ))
                .execute();
    }

    @Test(description = "Cancelling an order triggers full system flow: Postgres updated to CANCELLED, OrderCancelled event published to Kafka, MongoDB projection updated to CANCELLED")
    public void cancelOrder_fullSystemFlow() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        OrderDto created = Pipeline.given(TestOrderRequests.placeOrder(
                        TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(trackOrder())
                .execute();

        OrderDto cancelled = Pipeline.given(TestOrderRequests.cancelOrder(created.getUuid()))
                .then(httpClient.makeCall(200, OrderDto.class))
                .execute();

        Pipeline.given(cancelled)
                .then(Verify.allOf(
                        OrderTestMapper.toEntity().andThen(dbClient.findById()),
                        OrderTestMapper.toCancelledEvent().andThen(orderKafkaClient.consumeMatching(OrderCancelledEvent.class)),
                        OrderTestMapper.toCancelledProjectionDoc().andThen(mongoClient.findById())
                ))
                .execute();
    }
}
