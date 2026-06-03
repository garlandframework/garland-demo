package org.mtodemo.tests.infrastructure;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.Step;
import org.modulartestorchestrator.base.retry.RetryConfig;
import org.modulartestorchestrator.http.HttpTestClient;
import org.mtodemo.tests.dto.OrderDto;
import org.mtodemo.tests.dto.TokenDto;
import org.mtodemo.tests.dto.UserDto;
import org.mtodemo.tests.factory.TestAuthRequests;
import org.mtodemo.tests.factory.TestOrderRequests;
import org.mtodemo.tests.factory.TestUserRequests;
import org.modulartestorchestrator.kafka.KafkaConfig;
import org.modulartestorchestrator.kafka.KafkaTestClient;
import org.modulartestorchestrator.mongodb.MongoConfig;
import org.modulartestorchestrator.mongodb.MongoTestClient;
import org.modulartestorchestrator.mongodb.MongoWrapper;
import org.modulartestorchestrator.postgres.PostgresConfig;
import org.modulartestorchestrator.postgres.PostgresTestClient;
import org.modulartestorchestrator.postgres.PostgresWrapper;
import org.mtodemo.tests.document.OrderProjectionDoc;
import org.mtodemo.tests.document.UserProjectionDoc;
import org.mtodemo.tests.entity.AddressEntity;
import org.mtodemo.tests.entity.CarEntity;
import org.mtodemo.tests.entity.OrderEntity;
import org.mtodemo.tests.entity.OrderItemEntity;
import org.mtodemo.tests.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Listeners(TestLogger.class)
public abstract class BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    protected static HttpTestClient httpClient;
    protected static PostgresTestClient dbClient;
    protected static KafkaTestClient kafkaClient;
    protected static KafkaTestClient orderKafkaClient;
    protected static MongoTestClient mongoClient;

    protected static PostgresWrapper postgres;
    protected static MongoWrapper mongo;

    private final List<UUID> createdUserIds  = new ArrayList<>();
    private final List<UUID> createdOrderIds = new ArrayList<>();

    protected Step<UserDto, UserDto> trackUser() {
        return (dto, ctx) -> { createdUserIds.add(dto.getUuid()); return dto; };
    }

    protected Step<OrderDto, OrderDto> trackOrder() {
        return (dto, ctx) -> { createdOrderIds.add(dto.getUuid()); return dto; };
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupResources() {
        for (UUID id : new ArrayList<>(createdOrderIds)) {
            try {
                Pipeline.given(TestOrderRequests.cancelOrder(id))
                        .then(httpClient.makeCall(200, OrderDto.class))
                        .execute();
            } catch (Throwable e) {
                log.warn("Cleanup: failed to cancel order {}: {}", id, e.getMessage());
            }
        }
        createdOrderIds.clear();

        for (UUID id : new ArrayList<>(createdUserIds)) {
            try {
                Pipeline.given(TestUserRequests.deleteUser(id))
                        .then(httpClient.makeCall(204, Void.class))
                        .execute();
            } catch (Throwable e) {
                log.warn("Cleanup: failed to delete user {}: {}", id, e.getMessage());
            }
        }
        createdUserIds.clear();
    }

    @BeforeSuite
    public void setUpSuite() {
        if (httpClient != null) return;
        httpClient = new HttpTestClient(RetryConfig.of(3, Duration.ofSeconds(2)));
        TokenDto tokenDto = Pipeline.given(TestAuthRequests.login())
                .then(httpClient.makeCall(200, TokenDto.class))
                .execute();
        httpClient = httpClient.withBearer(tokenDto.token());

        postgres = new PostgresWrapper(
                PostgresConfig.builder()
                        .url(Connections.PG_URL)
                        .username(Connections.PG_USERNAME)
                        .password(Connections.PG_PASSWORD)
                        .entity(UserEntity.class)
                        .entity(AddressEntity.class)
                        .entity(CarEntity.class)
                        .entity(OrderEntity.class)
                        .entity(OrderItemEntity.class)
                        .build()
        );
        dbClient = new PostgresTestClient(postgres, RetryConfig.of(5, Duration.ofSeconds(2)))
                .withTemporalTolerance(Duration.ofNanos(1000));

        kafkaClient = new KafkaTestClient(
                KafkaConfig.builder()
                        .bootstrapServers(Connections.KAFKA_BOOTSTRAP_SERVERS)
                        .topic(Connections.KAFKA_TOPIC_USER_CREATED)
                        .topic(Connections.KAFKA_TOPIC_USER_UPDATED)
                        .topic(Connections.KAFKA_TOPIC_USER_DELETED)
                        .groupId(UUID.randomUUID().toString())
                        .build(),
                RetryConfig.of(5, Duration.ofSeconds(2))
        ).withTemporalTolerance(Duration.ofMillis(1));
        kafkaClient.warmup();

        orderKafkaClient = new KafkaTestClient(
                KafkaConfig.builder()
                        .bootstrapServers(Connections.KAFKA_BOOTSTRAP_SERVERS)
                        .topic(Connections.KAFKA_TOPIC_ORDER_PLACED)
                        .topic(Connections.KAFKA_TOPIC_ORDER_CANCELLED)
                        .groupId(UUID.randomUUID().toString())
                        .build(),
                RetryConfig.of(5, Duration.ofSeconds(2))
        ).withTemporalTolerance(Duration.ofMillis(1));
        orderKafkaClient.warmup();

        mongo = new MongoWrapper(
                MongoConfig.builder()
                        .connectionString(Connections.MONGO_CONNECTION_STRING)
                        .database(Connections.MONGO_DATABASE)
                        .collection(UserProjectionDoc.class, "users")
                        .collection(OrderProjectionDoc.class, "order_projections")
                        .build()
        );
        mongoClient = new MongoTestClient(mongo, RetryConfig.of(10, Duration.ofSeconds(2)))
                .withTemporalTolerance(Duration.ofMillis(1));
    }

    @BeforeTest
    public void seekKafkaToLatest() {
        if (kafkaClient != null) kafkaClient.warmup();
        if (orderKafkaClient != null) orderKafkaClient.warmup();
    }

    @AfterSuite
    public void tearDownSuite() {
        if (postgres != null) postgres.close();
        if (kafkaClient != null) kafkaClient.close();
        if (orderKafkaClient != null) orderKafkaClient.close();
        if (mongo != null) mongo.close();
    }
}
