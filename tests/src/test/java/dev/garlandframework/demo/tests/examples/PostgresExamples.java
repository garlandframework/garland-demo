package dev.garlandframework.demo.tests.examples;

import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.postgres.model.PostgresRequest;
import dev.garlandframework.demo.tests.support.base.BaseTest;
import dev.garlandframework.demo.tests.support.users.dto.UserDto;
import dev.garlandframework.demo.tests.support.users.entity.UserEntity;
import dev.garlandframework.demo.tests.support.users.factory.TestUserRequests;
import dev.garlandframework.demo.tests.support.users.mapper.UserTestMapper;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.UUID;

/**
 * Runnable examples of the Garland Postgres client API.
 * Each test demonstrates one pattern against the real local stack.
 *
 * Start services: docker-compose up -d
 * Run all:        mvn test -pl tests -Dtest=PostgresExamples
 * Run one:        mvn test -pl tests -Dtest=PostgresExamples#findById
 */
@Test(description = "Postgres client usage examples: find entity by id, temporal tolerance on timestamps, find by multiple fields, count-by-field assertion")
public class PostgresExamples extends BaseTest {

    // -------------------------------------------------------------------------
    // 1. findById — find entity by @Id field and assert it matches
    //
    //    The standard DB assertion pattern. The mapper converts the HTTP response
    //    DTO to an entity; findById looks it up by @Id and compares non-null fields.
    //    Retries until found — safe even when the write is slightly async.
    // -------------------------------------------------------------------------

    @Test(description = "Create user via HTTP — assert entity appears in Postgres with matching fields")
    public void findById() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(UserTestMapper.toEntity())
                .then(postgresClient.findById())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 2. findById — temporal tolerance override for a single call
    //
    //    The client default (set in BaseTest via withTemporalTolerance) covers
    //    microsecond truncation from Postgres. Use the Duration overload only when
    //    you need a larger tolerance for a specific call — e.g. asserting a
    //    service-generated timestamp is within a 1-second SLA window.
    // -------------------------------------------------------------------------

    @Test(description = "Assert entity timestamp is within 1-second tolerance — SLA-style assertion")
    public void findById_explicitTemporalTolerance() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(UserTestMapper.toEntity())
                .then(postgresClient.findById(Duration.ofSeconds(1)))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 3. findByFields — query by non-ID field values
    //
    //    Builds a WHERE clause from all non-null fields of the template entity.
    //    Use when the entity ID is not known before the query, or when you want
    //    to assert existence by business key rather than surrogate key.
    //    Throws if more than one row matches — narrow your criteria if needed.
    // -------------------------------------------------------------------------

    @Test(description = "Find user entity in Postgres by name and surname rather than by ID")
    public void findByFields() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserEntity template = UserEntity.builder()
                .name(created.getName())
                .surname(created.getSurname())
                .build(); // only non-null fields used as WHERE predicates

        Pipeline.given(template)
                .then(postgresClient.findByFields())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 4. countByFields — count rows matching a field template
    //
    //    Returns a Long; no assertion is performed by the step itself.
    //    Chain Verify.equalTo() to assert the count, or use the count
    //    value directly in downstream logic.
    // -------------------------------------------------------------------------

    @Test(description = "Assert exactly one row in Postgres matches the given name")
    public void countByFields() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserEntity template = UserEntity.builder()
                .name(created.getName())
                .build();

        Pipeline.given(template)
                .then(postgresClient.countByFields())
                .then(Verify.equalTo(1L))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 5. existsById — lighter presence check (no content assertion)
    //
    //    Use when you only need to know a row is there, not what it contains.
    //    Returns the input entity unchanged so it can continue through the pipeline.
    //    Retries until found according to the client's RetryConfig.
    // -------------------------------------------------------------------------

    @Test(description = "Assert the user entity exists in Postgres by ID — no content comparison")
    public void existsById() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .then(UserTestMapper.toEntity())
                .then(postgresClient.existsById())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 6. notExistsById — assert row is absent after deletion
    //
    //    No retry — deletion is synchronous, so absence is immediate.
    //    Pre-compute the entity before the DELETE call so you have the ID after
    //    the resource is gone.
    // -------------------------------------------------------------------------

    @Test(description = "Delete user via HTTP — assert entity is removed from Postgres")
    public void notExistsById() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserEntity entity = UserTestMapper.INSTANCE.toEntity(created); // pre-compute before deletion

        Pipeline.given(TestUserRequests.deleteUser(created.getUuid()))
                .then(httpClient.makeCall(204, Void.class))
                .execute();

        Pipeline.given(entity)
                .then(postgresClient.notExistsById())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 7. persist — direct DB insert bypassing the application layer
    //
    //    Use for test setup when you need a specific record in the database
    //    before the system-under-test runs. Not for asserting application
    //    behaviour — use findById after an HTTP call for that.
    //    Note: direct inserts bypass application logic (validation, events, etc.).
    // -------------------------------------------------------------------------

    @Test(description = "Insert a UserEntity directly into Postgres for test setup")
    public void persist() {
        UserEntity entity = UserEntity.builder()
                .id(UUID.randomUUID())
                .name("Setup")
                .surname("User")
                .build();

        Pipeline.given(PostgresRequest.persist(entity))
                .then(postgresClient.persist(entity))
                .execute();

        // entity is now in Postgres — use it to set up preconditions for the
        // system-under-test, then clean up manually after the test
        Pipeline.given(PostgresRequest.delete(UserEntity.class, entity.getId()))
                .then(postgresClient.delete())
                .execute();
    }
}
