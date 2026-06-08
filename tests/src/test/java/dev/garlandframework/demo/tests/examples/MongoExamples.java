package dev.garlandframework.demo.tests.examples;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.kafka.model.KafkaMessage;
import dev.garlandframework.mongodb.model.MongoRequest;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.users.document.UserProjectionDoc;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.event.UserCreatedEvent;
import dev.garlandframework.demo.tests.support.users.factory.TestUserEvents;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
import dev.garlandframework.demo.tests.support.users.mapper.UserTestMapper;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Runnable examples of the Garland MongoDB client API.
 * Each test demonstrates one pattern against the real local stack.
 *
 * Start services: docker-compose up -d
 * Run all:        mvn test -pl tests -Dtest=MongoExamples
 * Run one:        mvn test -pl tests -Dtest=MongoExamples#findById
 */
@Test(description = "MongoDB client usage examples: find by id, temporal tolerance on timestamps, find by field value, count-by-field assertion")
public class MongoExamples extends BaseTest {

    // -------------------------------------------------------------------------
    // 1. findById — find document by ID and assert it matches
    //
    //    The standard MongoDB assertion pattern. Retries until found — essential
    //    for projection-service documents that are written asynchronously after
    //    a Kafka event. The client's default tolerance (withTemporalTolerance)
    //    absorbs MongoDB's millisecond timestamp truncation automatically.
    // -------------------------------------------------------------------------

    @Test(description = "Create user via HTTP — assert projection document appears in MongoDB")
    public void findById() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(UserTestMapper.dtoToCreatedProjectionDoc()) // UserDto → UserProjectionDoc (expected)
                .then(mongoClient.findById())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 2. findById — explicit temporal tolerance override
    //
    //    The client default absorbs truncation (1ms). Use the Duration overload
    //    only when you need a larger tolerance for a specific call — for example,
    //    asserting that a service-generated eventTimestamp is within a 2-second
    //    SLA window rather than just correcting for precision loss.
    // -------------------------------------------------------------------------

    @Test(description = "Assert projection document appears in MongoDB — eventTimestamp within 2-second SLA")
    public void findById_explicitTemporalTolerance() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(UserTestMapper.dtoToCreatedProjectionDoc())
                .then(mongoClient.findById(Duration.ofSeconds(2)))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 3. findByFields — query by non-ID fields
    //
    //    Builds a query from all non-null fields of the template document.
    //    Use when the document ID is not known before the query, or when you
    //    want to assert presence by business field (e.g. fullName or sourceSystem).
    //    Throws if more than one document matches — narrow your criteria if needed.
    // -------------------------------------------------------------------------

    @Test(description = "Assert a user projection document exists in MongoDB by fullName field")
    public void findByFields() {
        UserCreatedEvent event = TestUserEvents.defaultUserCreatedEvent();

        Pipeline.given(new KafkaMessage<>(event.userId().toString(), event))
                .then(kafkaClient.publish())
                .execute();

        UserProjectionDoc template = UserProjectionDoc.builder()
                .fullName(event.fullName())
                .sourceSystem("user-service")
                .build(); // only non-null fields used as query predicates

        Pipeline.given(template)
                .then(mongoClient.findByFields())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 4. countByFields — count documents matching a field template
    //
    //    Returns a Long; no assertion is performed by the step itself.
    //    Chain Verify.equalTo() to assert the expected count.
    // -------------------------------------------------------------------------

    @Test(description = "Assert exactly one projection document exists in MongoDB for the given sourceSystem")
    public void countByFields() {
        UserCreatedEvent event = TestUserEvents.defaultUserCreatedEvent();

        Pipeline.given(new KafkaMessage<>(event.userId().toString(), event))
                .then(kafkaClient.publish())
                .execute();

        UserProjectionDoc template = UserProjectionDoc.builder()
                .id(event.userId())
                .build();

        Pipeline.given(template)
                .then(mongoClient.countByFields())
                .then(Verify.equalTo(1L))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 5. existsById — lighter presence check (no content assertion)
    //
    //    Use when you only need to confirm the document is present, not what
    //    it contains. Returns the input unchanged so the pipeline can continue.
    //    Retries until found according to the client's RetryConfig.
    // -------------------------------------------------------------------------

    @Test(description = "Assert the user projection document exists in MongoDB by ID — no content comparison")
    public void existsById() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(UserTestMapper.dtoToCreatedProjectionDoc())
                .then(mongoClient.existsById())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 6. notExistsById — assert document is absent
    //
    //    No retry — intended for synchronous deletions where absence is immediate.
    //    Pre-compute the expected document before the operation that removes it
    //    so you have the ID available for the assertion.
    // -------------------------------------------------------------------------

    @Test(description = "Delete user via HTTP — assert projection document is removed from MongoDB")
    public void notExistsById() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        // pre-compute the expected doc while we still have the data
        UserProjectionDoc expectedDoc = UserTestMapper.INSTANCE.toProjectionDoc(
                UserTestMapper.INSTANCE.toCreatedEvent(created));

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(expectedDoc)
                .then(mongoClient.notExistsById())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 7. persist — direct MongoDB insert bypassing the application layer
    //
    //    Use for test setup when you need a specific document in the collection
    //    before the system-under-test runs. Not for asserting application behaviour —
    //    use findById after publishing a Kafka event for that.
    //    Note: direct inserts bypass the projection pipeline (no Kafka involved).
    // -------------------------------------------------------------------------

    @Test(description = "Insert a UserProjectionDoc directly into MongoDB for test setup")
    public void persist() {
        UserCreatedEvent event = TestUserEvents.defaultUserCreatedEvent();
        UserProjectionDoc doc = UserTestMapper.INSTANCE.toProjectionDoc(event);

        Pipeline.given(MongoRequest.persist(doc))
                .then(mongoClient.persist(doc))
                .execute();

        // doc is now in MongoDB — use it to set up preconditions for the
        // system-under-test, then clean up after the test
        Pipeline.given(MongoRequest.delete(UserProjectionDoc.class, doc.getId()))
                .then(mongoClient.delete())
                .execute();
    }
}
