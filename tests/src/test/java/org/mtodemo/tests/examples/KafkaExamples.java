package org.mtodemo.tests.examples;

import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.checks.Verify;
import org.modulartestorchestrator.kafka.model.KafkaMessage;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.users.document.UserProjectionDoc;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.event.UserCreatedEvent;
import org.mtodemo.tests.support.users.factory.TestUserEvents;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.mtodemo.tests.support.users.mapper.UserTestMapper;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Runnable examples of the MTO Kafka client API.
 * Each test demonstrates one pattern against the real local stack.
 *
 * Start services: docker-compose up -d
 * Run all:        mvn test -pl tests -Dtest=KafkaExamples
 * Run one:        mvn test -pl tests -Dtest=KafkaExamples#consumeMatching
 *
 * IMPORTANT: All tests here share the Kafka topic. Run sequentially to prevent
 * event contamination — singleThreaded = true is set on the class for this reason.
 */
@Test(singleThreaded = true, description = "Kafka consumer usage examples: consume with content assertion, temporal SLA tolerance, and raw consumption without assertion")
public class KafkaExamples extends BaseTest {

    // -------------------------------------------------------------------------
    // 1. consumeMatching — the standard Kafka assertion pattern
    //
    //    The step input IS the expected value. Reads records with retry until
    //    one matches. Use this — rather than consume(type, expected) — when
    //    other events may arrive before the one you expect, because it tolerates
    //    interleaved messages by polling and comparing until the retry budget is
    //    exhausted.
    //    The client's default tolerance (withTemporalTolerance) is applied automatically.
    // -------------------------------------------------------------------------

    @Test(description = "Create user via HTTP — assert a matching UserCreated event appears on Kafka")
    public void consumeMatching() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(UserTestMapper.toCreatedEvent())    // UserDto → UserCreatedEvent (expected)
                .then(kafkaClient.consumeMatching(UserCreatedEvent.class))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 2. consumeMatching — explicit tolerance override
    //
    //    Use when a specific assertion needs a higher tolerance than the client
    //    default — for example, asserting that a service-generated eventTimestamp
    //    is within a 2-second SLA window rather than a 1ms truncation window.
    // -------------------------------------------------------------------------

    @Test(description = "Assert UserCreated event appears on Kafka — eventTimestamp within 2-second SLA")
    public void consumeMatching_explicitTolerance() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(UserTestMapper.toCreatedEvent())
                .then(kafkaClient.consumeMatching(UserCreatedEvent.class, Duration.ofSeconds(2)))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 3. consume (no assertion) — read next event, pass it downstream
    //
    //    Use when you want to receive the event and use it in subsequent steps
    //    but do not want to assert its content at the consume step itself.
    // -------------------------------------------------------------------------

    @Test(description = "Read the next UserCreated event from Kafka without asserting its content")
    public void consume_noAssertion() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        // consume the event triggered by the create above; pass to a downstream step
        Pipeline.given((Void) null)
                .then(kafkaClient.consume(UserCreatedEvent.class))
                .then(Verify.matching(new UserCreatedEvent(null, null, null, null, null, "user-service")))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 4. consume with fixed expected — when no interleaved messages are expected
    //
    //    Use when the topic is clean and you know exactly which event will arrive
    //    next. Slightly simpler than consumeMatching since you pass the expected
    //    value to consume() rather than having it flow through the pipeline.
    //    Prefer consumeMatching in shared topics where other events may arrive first.
    // -------------------------------------------------------------------------

    @Test(description = "Read next UserCreated event and assert it has sourceSystem = user-service")
    public void consume_withExpected() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserCreatedEvent expected = new UserCreatedEvent(null, null, null, null, null, "user-service");
        Pipeline.given((Void) null)
                .then(kafkaClient.consume(UserCreatedEvent.class, expected))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 5. publish — send a KafkaMessage directly to the default topic
    //
    //    The default topic is the first topic registered in KafkaConfig.
    //    kafkaClient → user.created topic.
    //    orderKafkaClient → order.placed topic.
    //    Use this pattern for Slice 2 tests (projection-service entry point) where
    //    you want to bypass user-service entirely.
    //
    //    Key format: string key used for Kafka partitioning — typically the entity ID.
    // -------------------------------------------------------------------------

    @Test(description = "Publish a UserCreated event directly to Kafka — bypasses user-service")
    public void publish() {
        UserCreatedEvent event = TestUserEvents.defaultUserCreatedEvent();

        Pipeline.given(new KafkaMessage<>(event.userId().toString(), event))
                .then(kafkaClient.publish())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 6. publish + downstream assertion — Kafka as entry point for projection tests
    //
    //    Publish an event directly, then assert the downstream projection appears
    //    in MongoDB. Use two separate pipelines: one to publish, one to assert.
    //    Do not chain publish and findById in a single pipeline — they are
    //    distinct operations with different input types.
    // -------------------------------------------------------------------------

    @Test(description = "Publish UserCreated event to Kafka — assert projection-service projects it to MongoDB")
    public void publish_assertedInMongo() {
        UserCreatedEvent event = TestUserEvents.defaultUserCreatedEvent();

        Pipeline.given(new KafkaMessage<>(event.userId().toString(), event))
                .then(kafkaClient.publish())
                .execute();

        UserProjectionDoc expectedDoc = UserTestMapper.INSTANCE.toProjectionDoc(event);
        Pipeline.given(expectedDoc)
                .then(mongoClient.findById())
                .execute();
    }
}
