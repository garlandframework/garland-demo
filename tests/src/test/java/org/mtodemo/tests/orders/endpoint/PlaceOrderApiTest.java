package org.mtodemo.tests.orders.endpoint;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.modulartestorchestrator.http.model.HttpCallRequest;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.common.dto.ValidationErrorDto;
import org.mtodemo.tests.support.orders.dto.OrderDto;
import org.mtodemo.tests.support.orders.factory.TestOrderItems;
import org.mtodemo.tests.support.orders.factory.TestOrderRequests;
import org.mtodemo.tests.support.orders.factory.TestOrders;
import org.mtodemo.tests.support.orders.mapper.OrderTestMapper;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

public class PlaceOrderApiTest extends BaseTest {

    @Test(description = "POST /api/orders with valid data returns 201 and order is persisted in Postgres")
    public void placeOrder_persistedInDb() {
        UserDto user = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        HttpCallRequest<OrderDto> request = TestOrderRequests.placeOrder(
                TestOrders.builder().userId(user.getUuid()).build());
        Pipeline.given(request)
                .then(httpClient.makeCall(201, OrderDto.class))
                .then(trackOrder())
                .then(Verify.matching(request.dto()))
                .then(OrderTestMapper.toEntity())
                .then(postgresClient.findById())
                .execute();
    }

    // --- required fields ---

    @Test(description = "Null userId is rejected with 400 and error pointing to 'userId' field")
    public void placeOrder_nullUserId_returns400() {
        Pipeline.given(TestOrderRequests.placeOrder(TestOrders.builder().userId(null).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("userId")))
                .execute();
    }

    @Test(description = "Null items list is rejected with 400 and error pointing to 'items' field")
    public void placeOrder_nullItems_returns400() {
        Pipeline.given(TestOrderRequests.placeOrder(TestOrders.builder().items(null).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items")))
                .execute();
    }

    @Test(description = "Empty items list is rejected with 400 and error pointing to 'items' field")
    public void placeOrder_emptyItems_returns400() {
        Pipeline.given(TestOrderRequests.placeOrder(TestOrders.builder().items(List.of()).build()))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items")))
                .execute();
    }

    // --- item.productName ---

    @Test(description = "Blank product name is rejected with 400 and error pointing to 'items[0].productName' field")
    public void placeOrder_blankProductName_returns400() {
        OrderDto payload = TestOrders.builder()
                .items(List.of(TestOrderItems.builder().productName("").build()))
                .build();
        Pipeline.given(TestOrderRequests.placeOrder(payload))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items[0].productName")))
                .execute();
    }

    @Test(description = "Null product name is rejected with 400 and error pointing to 'items[0].productName' field")
    public void placeOrder_nullProductName_returns400() {
        OrderDto payload = TestOrders.builder()
                .items(List.of(TestOrderItems.builder().productName(null).build()))
                .build();
        Pipeline.given(TestOrderRequests.placeOrder(payload))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items[0].productName")))
                .execute();
    }

    @Test(description = "Product name exceeding 200 characters is rejected with 400 and error pointing to 'items[0].productName' field")
    public void placeOrder_productNameTooLong_returns400() {
        OrderDto payload = TestOrders.builder()
                .items(List.of(TestOrderItems.builder().productName("a".repeat(201)).build()))
                .build();
        Pipeline.given(TestOrderRequests.placeOrder(payload))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items[0].productName")))
                .execute();
    }

    // --- item.quantity ---

    @Test(description = "Null quantity is rejected with 400 and error pointing to 'items[0].quantity' field")
    public void placeOrder_nullQuantity_returns400() {
        OrderDto payload = TestOrders.builder()
                .items(List.of(TestOrderItems.builder().quantity(null).build()))
                .build();
        Pipeline.given(TestOrderRequests.placeOrder(payload))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items[0].quantity")))
                .execute();
    }

    @Test(description = "Zero quantity is rejected with 400 and error pointing to 'items[0].quantity' field")
    public void placeOrder_zeroQuantity_returns400() {
        OrderDto payload = TestOrders.builder()
                .items(List.of(TestOrderItems.builder().quantity(0).build()))
                .build();
        Pipeline.given(TestOrderRequests.placeOrder(payload))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items[0].quantity")))
                .execute();
    }

    @Test(description = "Negative quantity is rejected with 400 and error pointing to 'items[0].quantity' field")
    public void placeOrder_negativeQuantity_returns400() {
        OrderDto payload = TestOrders.builder()
                .items(List.of(TestOrderItems.builder().quantity(-1).build()))
                .build();
        Pipeline.given(TestOrderRequests.placeOrder(payload))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items[0].quantity")))
                .execute();
    }

    // --- item.unitPrice ---

    @Test(description = "Null unit price is rejected with 400 and error pointing to 'items[0].unitPrice' field")
    public void placeOrder_nullUnitPrice_returns400() {
        OrderDto payload = TestOrders.builder()
                .items(List.of(TestOrderItems.builder().unitPrice(null).build()))
                .build();
        Pipeline.given(TestOrderRequests.placeOrder(payload))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items[0].unitPrice")))
                .execute();
    }

    @Test(description = "Zero unit price is rejected with 400 and error pointing to 'items[0].unitPrice' field")
    public void placeOrder_zeroUnitPrice_returns400() {
        OrderDto payload = TestOrders.builder()
                .items(List.of(TestOrderItems.builder().unitPrice(BigDecimal.ZERO).build()))
                .build();
        Pipeline.given(TestOrderRequests.placeOrder(payload))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items[0].unitPrice")))
                .execute();
    }

    @Test(description = "Negative unit price is rejected with 400 and error pointing to 'items[0].unitPrice' field")
    public void placeOrder_negativeUnitPrice_returns400() {
        OrderDto payload = TestOrders.builder()
                .items(List.of(TestOrderItems.builder().unitPrice(BigDecimal.valueOf(-1)).build()))
                .build();
        Pipeline.given(TestOrderRequests.placeOrder(payload))
                .then(httpClient.makeCall(400, ValidationErrorDto.class))
                .then(Verify.matching(ValidationErrorDto.forField("items[0].unitPrice")))
                .execute();
    }
}
