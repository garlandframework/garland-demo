package org.mtodemo.tests.infrastructure;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.retry.RetryConfig;
import org.modulartestorchestrator.http.HttpTestClient;
import org.mtodemo.tests.dto.TokenDto;
import org.mtodemo.tests.factory.TestAuthRequests;
import org.modulartestorchestrator.kafka.KafkaConfig;
import org.modulartestorchestrator.kafka.KafkaTestClient;
import org.modulartestorchestrator.mongodb.MongoConfig;
import org.modulartestorchestrator.mongodb.MongoTestClient;
import org.modulartestorchestrator.mongodb.MongoWrapper;
import org.modulartestorchestrator.postgres.DbConfig;
import org.modulartestorchestrator.postgres.DbTestClient;
import org.modulartestorchestrator.postgres.HibernateWrapper;
import org.mtodemo.tests.document.OrderProjectionDoc;
import org.mtodemo.tests.document.UserProjectionDoc;
import org.mtodemo.tests.entity.AddressEntity;
import org.mtodemo.tests.entity.CarEntity;
import org.mtodemo.tests.entity.OrderEntity;
import org.mtodemo.tests.entity.OrderItemEntity;
import org.mtodemo.tests.entity.UserEntity;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import java.time.Duration;
import java.util.UUID;

@Listeners(TestLogger.class)
public abstract class BaseTest {

    protected static HttpTestClient httpClient;
    protected static DbTestClient dbClient;
    protected static KafkaTestClient kafkaClient;
    protected static KafkaTestClient orderKafkaClient;
    protected static MongoTestClient mongoClient;

    protected static HibernateWrapper hibernate;
    protected static MongoWrapper mongo;

    @BeforeSuite
    public void setUpSuite() {
        if (httpClient != null) return;
        httpClient = new HttpTestClient(RetryConfig.of(3, Duration.ofSeconds(2)));
        TokenDto tokenDto = Pipeline.given(TestAuthRequests.login())
                .then(httpClient.makeCall(200, TokenDto.class))
                .execute();
        httpClient = httpClient.withBearer(tokenDto.token());

        hibernate = new HibernateWrapper(
                DbConfig.builder()
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
        dbClient = new DbTestClient(hibernate, RetryConfig.of(5, Duration.ofSeconds(2)));

        kafkaClient = new KafkaTestClient(
                KafkaConfig.builder()
                        .bootstrapServers(Connections.KAFKA_BOOTSTRAP_SERVERS)
                        .topic(Connections.KAFKA_TOPIC_USER_CREATED)
                        .topic(Connections.KAFKA_TOPIC_USER_UPDATED)
                        .topic(Connections.KAFKA_TOPIC_USER_DELETED)
                        .groupId(UUID.randomUUID().toString())
                        .build(),
                RetryConfig.of(5, Duration.ofSeconds(2))
        );
        kafkaClient.warmup();

        orderKafkaClient = new KafkaTestClient(
                KafkaConfig.builder()
                        .bootstrapServers(Connections.KAFKA_BOOTSTRAP_SERVERS)
                        .topic(Connections.KAFKA_TOPIC_ORDER_PLACED)
                        .topic(Connections.KAFKA_TOPIC_ORDER_CANCELLED)
                        .groupId(UUID.randomUUID().toString())
                        .build(),
                RetryConfig.of(5, Duration.ofSeconds(2))
        );
        orderKafkaClient.warmup();

        mongo = new MongoWrapper(
                MongoConfig.builder()
                        .connectionString(Connections.MONGO_CONNECTION_STRING)
                        .database(Connections.MONGO_DATABASE)
                        .collection(UserProjectionDoc.class, "users")
                        .collection(OrderProjectionDoc.class, "order_projections")
                        .build()
        );
        mongoClient = new MongoTestClient(mongo, RetryConfig.of(10, Duration.ofSeconds(2)));
    }

    @AfterSuite
    public void tearDownSuite() {
        if (hibernate != null) hibernate.close();
        if (kafkaClient != null) kafkaClient.close();
        if (orderKafkaClient != null) orderKafkaClient.close();
        if (mongo != null) mongo.close();
    }
}
