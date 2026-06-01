package org.mtodemo.tests.orders;

import org.modulartestorchestrator.base.Pipeline;
import org.mtodemo.tests.document.OrderProjectionDoc;
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

public class OrderEndToEndTest extends BaseTest {

    @Test(description = "Placing an order triggers full system flow: persisted in Postgres, OrderPlaced event published to Kafka, projected into MongoDB")
    public void placeOrder_fullSystemFlow() throws Exception {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .execute();

        Pipeline.given(TestOrderRequests.placeOrder(TestOrders.builder().userId(user.getUuid()).build()))
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(OrderTestMapper.toEntity())
                .then(dbClient.findById())
                .then(OrderTestMapper.entityToPlacedEvent())
                .then(orderKafkaClient.consumeMatching(OrderPlacedEvent.class))
                .then(OrderTestMapper.toProjectionDoc())
                .then(mongoClient.findById())
                .execute();
    }

    @Test(description = "Cancelling an order triggers full system flow: Postgres updated to CANCELLED, OrderCancelled event published to Kafka, MongoDB projection updated to CANCELLED")
    public void cancelOrder_fullSystemFlow() throws Exception {
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
                .then(OrderTestMapper.toEntity())
                .then(dbClient.findById())
                .execute();

        Pipeline.given(cancelled)
                .then(OrderTestMapper.toCancelledEvent())
                .then(orderKafkaClient.consumeMatching(OrderCancelledEvent.class))
                .execute();

        OrderProjectionDoc expectedCancelledDoc = OrderProjectionDoc.builder()
                .id(created.getUuid())
                .status("CANCELLED")
                .build();
        Pipeline.given(expectedCancelledDoc)
                .then(mongoClient.findById())
                .execute();
    }
}
