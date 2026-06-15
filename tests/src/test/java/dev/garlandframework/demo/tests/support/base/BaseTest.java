package dev.garlandframework.demo.tests.support.base;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.Step;
import dev.garlandframework.base.retry.RetryConfig;
import dev.garlandframework.base.tracker.ResourceTracker;
import dev.garlandframework.http.HttpTestClient;
import dev.garlandframework.demo.tests.support.common.dto.TokenDto;
import dev.garlandframework.demo.tests.support.common.factory.TestAuthRequests;
import dev.garlandframework.demo.tests.support.users.document.UserProjectionDoc;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.entity.AddressEntity;
import dev.garlandframework.demo.tests.support.users.entity.CarEntity;
import dev.garlandframework.demo.tests.support.users.entity.UserEntity;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
import dev.garlandframework.demo.tests.support.users.mapper.UserTestMapper;
import dev.garlandframework.kafka.KafkaConfig;
import dev.garlandframework.kafka.KafkaTestClient;
import dev.garlandframework.mongodb.MongoConfig;
import dev.garlandframework.mongodb.MongoTestClient;
import dev.garlandframework.mongodb.MongoWrapper;
import dev.garlandframework.postgres.PostgresConfig;
import dev.garlandframework.postgres.PostgresTestClient;
import dev.garlandframework.postgres.PostgresWrapper;
import dev.garlandframework.testng.AbstractGarlandBaseTest;
import dev.garlandframework.testng.TestNGLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;

import java.time.Duration;
import java.util.UUID;

@Listeners(TestNGLogger.class)
public abstract class BaseTest extends AbstractGarlandBaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    protected static HttpTestClient httpClient;
    protected static PostgresTestClient postgresClient;
    protected static KafkaTestClient kafkaClient;
    protected static MongoTestClient mongoClient;

    protected static PostgresWrapper postgres;
    protected static MongoWrapper mongo;

    protected final ResourceTracker<UUID> userTracker = new ResourceTracker<>(
            id -> Pipeline.given(TestUserRequests.deleteUser(id))
                          .then(httpClient.makeCall(204, Void.class))
                          .execute()
    );

    protected BaseTest() {
        registerTrackers(userTracker);
    }

    protected Step<UserDto, UserDto> trackUser() {
        return userTracker.track(UserDto::getUuid);
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
        log.info("=== Stage 2 passed: Kafka consumers have partition assignment ===");

        mongo = new MongoWrapper(
                MongoConfig.builder()
                        .connectionString(Connections.MONGO_CONNECTION_STRING)
                        .database(Connections.MONGO_DATABASE)
                        .collection(UserProjectionDoc.class, "users")
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
    }

    @AfterSuite
    public void tearDownSuite() {
        if (postgres != null) postgres.close();
        if (kafkaClient != null) kafkaClient.close();
        if (mongo != null) mongo.close();
    }
}
