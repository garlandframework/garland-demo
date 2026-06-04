package org.mtodemo.tests.support.base;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.Step;
import org.modulartestorchestrator.base.retry.RetryConfig;
import org.modulartestorchestrator.http.HttpTestClient;
import org.mtodemo.tests.support.common.dto.TokenDto;
import org.mtodemo.tests.support.common.factory.TestAuthRequests;
import org.mtodemo.tests.support.orders.document.OrderProjectionDoc;
import org.mtodemo.tests.support.orders.dto.OrderDto;
import org.mtodemo.tests.support.orders.factory.TestOrderRequests;
import org.mtodemo.tests.support.users.document.UserProjectionDoc;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.entity.AddressEntity;
import org.mtodemo.tests.support.users.entity.CarEntity;
import org.mtodemo.tests.support.users.entity.UserEntity;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.mtodemo.tests.support.users.mapper.UserTestMapper;
import org.mtodemo.tests.support.orders.entity.OrderEntity;
import org.mtodemo.tests.support.orders.entity.OrderItemEntity;
import org.modulartestorchestrator.kafka.KafkaConfig;
import org.modulartestorchestrator.kafka.KafkaTestClient;
import org.modulartestorchestrator.mongodb.MongoConfig;
import org.modulartestorchestrator.mongodb.MongoTestClient;
import org.modulartestorchestrator.mongodb.MongoWrapper;
import org.modulartestorchestrator.postgres.PostgresConfig;
import org.modulartestorchestrator.postgres.PostgresTestClient;
import org.modulartestorchestrator.postgres.PostgresWrapper;
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
    protected static PostgresTestClient postgresClient;
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

        new EnvironmentReadinessChecker().waitForServicesHealthy();

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
        postgresClient = new PostgresTestClient(postgres, RetryConfig.of(5, Duration.ofSeconds(2)))
                .withTemporalTolerance(Duration.ofNanos(1000));

        log.info("=== Stage 2: warming up Kafka test consumers ===");
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
        log.info("  kafkaClient (user topics) assigned");

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
        log.info("  orderKafkaClient (order topics) assigned");
        log.info("=== Stage 2 passed: Kafka consumers have partition assignment ===");

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

        runSmokeProbe();
    }

    private static void runSmokeProbe() {
        log.info("=== Stage 3: running end-to-end smoke probe ===");
        UserDto user = null;
        try {
            user = Pipeline.given(TestUserRequests.createUser())
                    .then(httpClient.makeCall(201, UserDto.class))
                    .execute();

            MongoTestClient probeMongoClient = new MongoTestClient(
                    mongo, RetryConfig.of(30, Duration.ofSeconds(3))
            ).withTemporalTolerance(Duration.ofMillis(1));

            Pipeline.given(user)
                    .then(UserTestMapper.dtoToCreatedProjectionDoc())
                    .then(probeMongoClient.findById())
                    .execute();

            log.info("=== Stage 3 passed: full pipeline is operational — tests may begin ===");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Smoke probe failed — pipeline not ready: " + e.getMessage(), e);
        } finally {
            if (user != null) {
                try {
                    Pipeline.given(TestUserRequests.deleteUser(user.getUuid()))
                            .then(httpClient.makeCall(204, Void.class))
                            .execute();
                } catch (Exception ignored) {
                    log.warn("Smoke probe cleanup: could not delete probe user {}", user.getUuid());
                }
            }
        }
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
