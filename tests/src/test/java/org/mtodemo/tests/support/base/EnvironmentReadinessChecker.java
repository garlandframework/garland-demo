package org.mtodemo.tests.support.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

public class EnvironmentReadinessChecker {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentReadinessChecker.class);

    private static final Duration POLL_INTERVAL  = Duration.ofSeconds(2);
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(120);

    private final HttpClient   rawHttpClient;
    private final ObjectMapper objectMapper;

    public EnvironmentReadinessChecker() {
        this.rawHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Stage 1 — polls /actuator/health on both services until both report status=UP.
     * Spring Boot only reports UP when all health indicators pass (DB, Kafka, MongoDB).
     * Blocks for up to 120 s per service; throws if a service never becomes healthy.
     */
    public void waitForServicesHealthy() {
        log.info("=== Stage 1: waiting for service health endpoints ===");
        waitForHealth("user-service",
                Connections.USER_SERVICE_URL + "/actuator/health");
        waitForHealth("projection-service",
                Connections.PROJECTION_SERVICE_URL + "/actuator/health");
        log.info("=== Stage 1 passed: both services are UP ===");
    }

    private void waitForHealth(String name, String url) {
        log.info("  Polling {} at {} ...", name, url);
        Instant deadline  = Instant.now().plus(HEALTH_TIMEOUT);
        String  lastError = "no response yet";

        while (Instant.now().isBefore(deadline)) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();
                HttpResponse<String> response =
                        rawHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode body   = objectMapper.readTree(response.body());
                    String   status = body.path("status").asText();
                    if ("UP".equals(status)) {
                        log.info("  {} is UP", name);
                        return;
                    }
                    lastError = "status=" + status + " body=" + response.body();
                } else {
                    lastError = "HTTP " + response.statusCode();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for " + name);
            } catch (Exception e) {
                lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            }

            log.debug("    {} not ready ({}), retrying in {}s ...",
                    name, lastError, POLL_INTERVAL.toSeconds());
            try {
                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        throw new IllegalStateException(
                "Environment not ready: " + name + " did not become healthy within "
                + HEALTH_TIMEOUT.toSeconds() + "s. Last error: " + lastError);
    }
}
