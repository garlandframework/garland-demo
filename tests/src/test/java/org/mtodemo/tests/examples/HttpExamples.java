package org.mtodemo.tests.examples;

import com.fasterxml.jackson.core.type.TypeReference;
import org.modulartestorchestrator.base.Pipeline;
import org.modulartestorchestrator.base.retry.RetryConfig;
import org.modulartestorchestrator.http.HttpTestClient;
import org.modulartestorchestrator.http.model.FormBody;
import org.modulartestorchestrator.http.model.Header;
import org.modulartestorchestrator.http.model.HttpCallRequest;
import org.modulartestorchestrator.http.model.HttpCallResponse;
import org.modulartestorchestrator.http.model.MultipartBody;
import org.mtodemo.tests.support.base.BaseTest;
import org.mtodemo.tests.support.base.Connections;
import org.mtodemo.tests.support.common.dto.ErrorDto;
import org.mtodemo.tests.support.common.dto.TokenDto;
import org.mtodemo.tests.support.common.factory.TestAuthRequests;
import org.mtodemo.tests.support.users.dto.UserDto;
import org.mtodemo.tests.support.users.factory.TestUserRequests;
import org.mtodemo.tests.support.users.factory.TestUsers;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runnable examples of the MTO HTTP client API.
 * Each test demonstrates one pattern against the real local stack.
 *
 * Start services: docker-compose up -d
 * Run all:        mvn test -pl tests -Dtest=HttpExamples
 * Run one:        mvn test -pl tests -Dtest=HttpExamples#makeCall_statusOnly
 */
public class HttpExamples extends BaseTest {

    // =========================================================================
    // makeCall
    // =========================================================================

    // -------------------------------------------------------------------------
    // 1. makeCall — assert status, return deserialized body
    //
    //    The most common pattern. Use when you need the response object
    //    but will assert its content in a later step.
    // -------------------------------------------------------------------------

    @Test(description = "POST returns 201 — status asserted, body returned for downstream use")
    public void makeCall_statusOnly() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        // created.getUuid() is populated from the response — use it in follow-up requests
    }

    // -------------------------------------------------------------------------
    // 2. makeCall — assert status + body shape
    //
    //    HttpCallResponse describes the expected response. Null fields in the
    //    expected DTO are ignored — only non-null fields are compared.
    //    Use this when you want to assert the response shape in the same step.
    // -------------------------------------------------------------------------

    @Test(description = "POST returns 201 — status and response body shape both asserted")
    public void makeCall_withExpectedBody() {
        UserDto request = TestUsers.defaultUser();
        UserDto expected = UserDto.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .build(); // uuid/createdAt left null → ignored by matchingNonNull

        Pipeline.given(TestUserRequests.createUser(request))
                .then(httpClient.makeCall(new HttpCallResponse<>(201, Map.of(), expected)))
                .then(trackUser())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 3. makeCall — temporal tolerance for server-generated timestamps
    //
    //    When the expected DTO contains timestamps (createdAt, modifiedAt) that
    //    are server-generated, use the Duration overload to allow small drift.
    // -------------------------------------------------------------------------

    @Test(description = "POST returns 201 — timestamp fields compared with 1-second tolerance")
    public void makeCall_withTemporalTolerance() {
        UserDto request = TestUsers.defaultUser();
        UserDto expected = UserDto.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .createdAt(java.time.LocalDateTime.now())  // approximate; tolerance covers the gap
                .build();

        Pipeline.given(TestUserRequests.createUser(request))
                .then(httpClient.makeCall(new HttpCallResponse<>(201, Map.of(), expected), Duration.ofSeconds(1)))
                .then(trackUser())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 4. makeCall — generic response type (List<T>)
    //
    //    Use TypeReference when the response type is generic (e.g. List<UserDto>).
    //    A plain Class<List> would erase the element type and break deserialization.
    // -------------------------------------------------------------------------

    @Test(description = "GET /api/users returns 200 and a typed list of users")
    public void makeCall_genericListResponse() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        List<UserDto> users = Pipeline.given(TestUserRequests.getAllUsers())
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .execute();

        // users is a fully typed List<UserDto> — iterate, assert, pass downstream
    }

    // -------------------------------------------------------------------------
    // 5. makeCall — error response (4xx)
    //
    //    Same pattern for error paths. The error body is deserialized just like
    //    a success body — use an error DTO that mirrors the server's error shape.
    // -------------------------------------------------------------------------

    @Test(description = "GET /api/users/{id} with unknown id returns 404 with error body")
    public void makeCall_notFound() {
        Pipeline.given(TestUserRequests.getUser(UUID.randomUUID()))
                .then(httpClient.makeCall(404, ErrorDto.class))
                .execute();
    }

    // =========================================================================
    // pollingCall
    // =========================================================================

    // -------------------------------------------------------------------------
    // 6. pollingCall — eventually-consistent endpoints
    //
    //    Retries the call until the response body matches the expected DTO,
    //    up to maxAttempts times with the given delay between attempts.
    //    Use for read-model endpoints populated by async consumers.
    // -------------------------------------------------------------------------

    @Test(description = "GET /api/users/{id} polled until it returns the expected body")
    public void pollingCall_retryUntilMatch() {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        UserDto expected = UserDto.builder()
                .uuid(created.getUuid())
                .name(created.getName())
                .build();

        Pipeline.given(TestUserRequests.getUser(created.getUuid()))
                .then(httpClient.pollingCall(200, expected, RetryConfig.of(5, Duration.ofMillis(500))))
                .execute();
    }

    // =========================================================================
    // Auth
    // =========================================================================

    // -------------------------------------------------------------------------
    // 7. withoutHeader — remove a header for a single call (negative auth test)
    //
    //    The suite-wide httpClient already has Authorization set in @BeforeSuite.
    //    withoutHeader returns a new client with the header removed — the original
    //    is unchanged. Use this for negative auth tests that must send no token.
    // -------------------------------------------------------------------------

    @Test(description = "Request without Authorization header returns 401")
    public void auth_noToken_returns401() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.withoutHeader("Authorization").makeCall(401, ErrorDto.class))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 8. withBearer — replace the suite-wide token for a single call
    //
    //    withBearer returns a new client with a different Authorization header.
    //    Use when a specific test needs a different token than the suite default —
    //    for example, a wrong token to verify rejection.
    // -------------------------------------------------------------------------

    @Test(description = "Request with an invalid JWT returns 401")
    public void auth_invalidToken_returns401() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.withBearer("not-a-valid-jwt").makeCall(401, ErrorDto.class))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 9. storeBearer — token fetched inside the pipeline
    //
    //    Use when the token is obtained mid-pipeline rather than beforehand.
    //    storeBearer stores the token in PipelineContext; every subsequent
    //    makeCall on the same client picks it up as Authorization: Bearer <token>.
    //    Client-level headers (withBearer) take priority over context tokens.
    // -------------------------------------------------------------------------

    @Test(description = "Login, store token in pipeline context, then use it for a subsequent call")
    public void auth_storeBearerInPipeline() {
        HttpTestClient unauthenticated = new HttpTestClient();

        TokenDto tokenDto = Pipeline.given(TestAuthRequests.login())
                .then(unauthenticated.makeCall(200, TokenDto.class))
                .then(HttpTestClient.storeBearer(TokenDto::token))
                .execute();

        // any subsequent makeCall on 'unauthenticated' in the same pipeline
        // would carry Authorization: Bearer <token> from context automatically.
        Pipeline.given(TestUserRequests.createUser())
                .then(unauthenticated.withBearer(tokenDto.token()).makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    // =========================================================================
    // Query parameters
    // =========================================================================

    // -------------------------------------------------------------------------
    // 10. withQueryParam — single parameter
    //
    //     Appends one percent-encoded parameter to the URL at call time.
    //     The factory method stays clean; tests inject valid, invalid, or
    //     malformed values on top without touching the factory.
    // -------------------------------------------------------------------------

    @Test(description = "GET /api/users with a single query parameter")
    public void queryParam_single() {
        List<UserDto> users = Pipeline.given(
                        TestUserRequests.getAllUsers().withQueryParam("page", "0"))
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 11. withQueryParams — multiple parameters at once
    //
    //     Use in factory methods when a request naturally carries several
    //     parameters. Cleaner than chaining withQueryParam for each one.
    //     All entries are merged; existing keys are replaced.
    // -------------------------------------------------------------------------

    @Test(description = "GET /api/users with multiple query parameters at once")
    public void queryParam_multiple() {
        List<UserDto> users = Pipeline.given(
                        TestUserRequests.getAllUsers()
                                .withQueryParams(Map.of("page", "0", "size", "10")))
                .then(httpClient.makeCall(200, new TypeReference<List<UserDto>>() {}))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 12. withQueryParam — adversarial: invalid value
    //
    //     The factory produces the base request; the test overrides one param
    //     with a bad value. The server rejects it and returns 400.
    //     Same pattern for malformed ("abc" instead of a number) or too-long values.
    // -------------------------------------------------------------------------

    @Test(description = "GET /api/users with invalid page param returns 400")
    public void queryParam_invalidValue() {
        Pipeline.given(TestUserRequests.getAllUsers().withQueryParam("page", "-1"))
                .then(httpClient.makeCall(400, ErrorDto.class))
                .execute();
    }

    // =========================================================================
    // Content-Type bodies
    // =========================================================================

    // -------------------------------------------------------------------------
    // 13. FormBody — application/x-www-form-urlencoded
    //
    //     Pass a FormBody as the dto. HttpSteps detects it and encodes all fields
    //     as percent-encoded key=value pairs. Content-Type is set automatically.
    //     Typical use: OAuth2 token endpoints, legacy form APIs.
    //
    //     Disabled: this demo project has no form-encoded endpoint.
    // -------------------------------------------------------------------------

    @Test(enabled = false, description = "POST with form-encoded body — OAuth2 token endpoint pattern")
    public void formBody_oauthTokenRequest() {
        TokenDto token = Pipeline.given(
                        new HttpCallRequest<>(
                                "http://localhost:8080/oauth/token",
                                "POST",
                                List.of(),
                                new FormBody()
                                        .field("grant_type", "client_credentials")
                                        .field("client_id", "my-client")
                                        .field("client_secret", "secret")))
                .then(httpClient.makeCall(200, TokenDto.class))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 14. MultipartBody — multipart/form-data, file from disk
    //
    //     Pass a MultipartBody as the dto. HttpSteps builds the multipart byte
    //     stream with a random boundary. Content-Type is set automatically.
    //     Typical use: file upload endpoints.
    //
    //     Disabled: this demo project has no multipart endpoint.
    // -------------------------------------------------------------------------

    @Test(enabled = false, description = "POST multipart/form-data with a text field and a file from disk")
    public void multipartBody_fileUpload() throws Exception {
        Pipeline.given(
                        new HttpCallRequest<>(
                                "http://localhost:8080/api/files",
                                "POST",
                                List.of(),
                                new MultipartBody()
                                        .field("description", "profile photo")
                                        .file("photo", Path.of("/tmp/photo.jpg"), "image/jpeg")))
                .then(httpClient.makeCall(201, Void.class))
                .execute();
    }

    // -------------------------------------------------------------------------
    // 15. MultipartBody — multipart/form-data, file from in-memory bytes
    //
    //     Use when the file content is already in memory (generated in the test,
    //     read from a resource, etc.) — no file on disk required.
    //
    //     Disabled: this demo project has no multipart endpoint.
    // -------------------------------------------------------------------------

    @Test(enabled = false, description = "POST multipart/form-data with in-memory bytes — no file on disk needed")
    public void multipartBody_inMemoryBytes() throws Exception {
        byte[] data = "hello world".getBytes();

        Pipeline.given(
                        new HttpCallRequest<>(
                                "http://localhost:8080/api/files",
                                "POST",
                                List.of(),
                                new MultipartBody()
                                        .field("label", "greeting")
                                        .file("content", data, "hello.txt", "text/plain")))
                .then(httpClient.makeCall(201, Void.class))
                .execute();
    }

    // =========================================================================
    // Raw string body
    // =========================================================================

    // -------------------------------------------------------------------------
    // 16. Raw JSON string body
    //
    //     Pass a String as dto — HttpSteps skips Jackson serialization and sends
    //     the string as-is. Content-Type defaults to application/json.
    //     Use when the body comes from a fixture file, a captured replay, or is
    //     built by string interpolation in a parameterized test.
    // -------------------------------------------------------------------------

    @Test(description = "POST with a pre-serialized JSON string body — skips Jackson")
    public void rawStringBody_prebuiltJson() {
        String json = "{\"name\":\"Alice\",\"surname\":\"Smith\"}";

        UserDto created = Pipeline.given(
                        new HttpCallRequest<>(
                                Connections.USER_SERVICE_URL + "/api/users",
                                "POST",
                                List.of(),
                                json))
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 17. Raw non-JSON string body (XML, plain text, etc.)
    //
    //     Same as above, but with a Content-Type header to override the default
    //     application/json. Use for any content type that is already a string.
    //
    //     Disabled: this demo project has no XML endpoint.
    // -------------------------------------------------------------------------

    @Test(enabled = false, description = "POST with a raw XML body — Content-Type overrides the default")
    public void rawStringBody_xml() {
        String xml = "<user><name>Alice</name><surname>Smith</surname></user>";

        Pipeline.given(
                        new HttpCallRequest<>(
                                Connections.USER_SERVICE_URL + "/api/users",
                                "POST",
                                List.of(new Header("Content-Type", "application/xml")),
                                xml))
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    // =========================================================================
    // Base URL
    // =========================================================================

    // -------------------------------------------------------------------------
    // 18. withBaseUrl — relative-path request factories
    //
    //     withBaseUrl returns a new client that prepends the host to any request
    //     URL starting with '/'. Absolute URLs are used as-is.
    //     All other client settings (headers, auth, retry) are carried over.
    //     Use when you want to change the host in one place (e.g. environments).
    // -------------------------------------------------------------------------

    @Test(description = "Client with base URL — request uses a relative path, host prepended at call time")
    public void baseUrl_relativePathRequest() {
        HttpTestClient client = httpClient.withBaseUrl(Connections.USER_SERVICE_URL);

        UserDto created = Pipeline.given(
                        new HttpCallRequest<>("/api/users", "POST", List.of(), TestUsers.defaultUser()))
                .then(client.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 19. withBaseUrl + withBearer — chaining preserves all settings
    //
    //     All with* methods carry the base URL forward. Here a fresh client is
    //     built from scratch: login to get a token, then configure base URL and
    //     bearer auth. Demonstrates that chaining order does not matter.
    // -------------------------------------------------------------------------

    @Test(description = "withBaseUrl and withBearer chain — all settings are preserved across mutations")
    public void baseUrl_chainedWithAuth() {
        TokenDto tokenDto = Pipeline.given(TestAuthRequests.login())
                .then(new HttpTestClient().makeCall(200, TokenDto.class))
                .execute();

        HttpTestClient client = new HttpTestClient()
                .withBaseUrl(Connections.USER_SERVICE_URL)
                .withBearer(tokenDto.token());

        UserDto created = Pipeline.given(
                        new HttpCallRequest<>("/api/users", "POST", List.of(), TestUsers.defaultUser()))
                .then(client.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    // =========================================================================
    // Timeout
    // =========================================================================

    // -------------------------------------------------------------------------
    // 20. withTimeout — per-client request timeout
    //
    //     Without a timeout a hanging server blocks the test thread forever.
    //     withTimeout applies HttpRequest.Builder.timeout() to every call on
    //     this client. If the server does not respond in time,
    //     HttpTimeoutException is thrown (wrapped in RuntimeException).
    // -------------------------------------------------------------------------

    @Test(description = "Client with a 10-second timeout — hangs fail fast instead of blocking forever")
    public void timeout_callCompletesWithinLimit() {
        HttpTestClient client = httpClient.withTimeout(Duration.ofSeconds(10));

        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(client.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    // -------------------------------------------------------------------------
    // 21. withTimeout + withBaseUrl — all with* settings chain together
    //
    //     withTimeout carries forward through all other with* mutations.
    //     Combine with withBaseUrl, withBearer, etc. in any order.
    // -------------------------------------------------------------------------

    @Test(description = "withTimeout and withBaseUrl chained — both settings applied to every call")
    public void timeout_chainedWithBaseUrl() {
        HttpTestClient client = httpClient
                .withBaseUrl(Connections.USER_SERVICE_URL)
                .withTimeout(Duration.ofSeconds(10));

        UserDto created = Pipeline.given(
                        new HttpCallRequest<>("/api/users", "POST", List.of(), TestUsers.defaultUser()))
                .then(client.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    // =========================================================================
    // Cookies
    // =========================================================================

    // -------------------------------------------------------------------------
    // 22. withCookie — convenience over withHeader("Cookie", ...)
    //
    //     Formats the Cookie header as name=value automatically. Use for
    //     session-cookie auth schemes or services that read cookies directly.
    //     Chain multiple withCookie calls to send more than one cookie.
    //
    //     Disabled: this demo project uses Bearer auth, not cookies.
    // -------------------------------------------------------------------------

    @Test(enabled = false, description = "Request with a session cookie — cookie-auth endpoint pattern")
    public void cookie_sessionAuth() {
        Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.withCookie("session", "abc123").makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();
    }

    // =========================================================================
    // File download
    // =========================================================================

    // -------------------------------------------------------------------------
    // 23. downloadFile — save binary response to disk
    //
    //     Uses BodyHandlers.ofByteArray() so binary content (PDF, image, ZIP)
    //     is never corrupted by charset conversion. Parent directories are
    //     created automatically. Returns the Path for further assertions.
    //
    //     Disabled: this demo project has no file download endpoint.
    // -------------------------------------------------------------------------

    @Test(description = "GET /api/users/{id}/export — binary CSV response saved to disk as-is")
    public void downloadFile_savesToDisk() throws Exception {
        UserDto created = Pipeline.given(TestUserRequests.createUser())
                .then(httpClient.makeCall(201, UserDto.class))
                .then(trackUser())
                .execute();

        Path destination = Path.of(System.getProperty("java.io.tmpdir"), "user-" + created.getUuid() + ".csv");

        Path saved = Pipeline.given(
                        new HttpCallRequest<>(
                                Connections.USER_SERVICE_URL + "/api/users/" + created.getUuid() + "/export",
                                "GET",
                                List.of(),
                                null))
                .then(httpClient.downloadFile(200, destination))
                .execute();

        assert Files.size(saved) > 0;
    }
}
