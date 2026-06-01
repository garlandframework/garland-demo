package org.mtodemo.tests.orders;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.mtodemo.tests.dto.OrderDto;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.event.OrderCancelledEvent;
import org.mtodemo.tests.event.OrderPlacedEvent;
import org.mtodemo.tests.factory.TestOrderRequests;
import org.mtodemo.tests.factory.TestOrders;
import org.mtodemo.tests.factory.TestUserRequests;
import org.mtodemo.tests.infrastructure.BaseTest;
import org.mtodemo.tests.mapper.OrderTestMapper;
import org.testng.annotations.Test;

import java.time.Duration;

public class OrderEndToEndTest extends BaseTest {

    @Test(description = "Placing an order triggers full system flow: persisted in Postgres, OrderPlaced event published to Kafka, projected into MongoDB")
    public void placeOrder_fullSystemFlow() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        Pipeline.given(TestOrderRequests.placeOrder(TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(Verify.allOf(
                        OrderTestMapper.toEntity().andThen(dbClient.findById()),
                        OrderTestMapper.toPlacedEvent().andThen(orderKafkaClient.consumeMatching(OrderPlacedEvent.class)),
                        OrderTestMapper.toPlacedEvent().andThen(OrderTestMapper.toProjectionDoc()).andThen(mongoClient.findById(Duration.ofMillis(1)))
                ))
                .execute();
    }

    @Test(description = "Cancelling an order triggers full system flow: Postgres updated to CANCELLED, OrderCancelled event published to Kafka, MongoDB projection updated to CANCELLED")
    public void cancelOrder_fullSystemFlow() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        OrderDto created = Pipeline.given(TestOrderRequests.placeOrder(
                        TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
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
