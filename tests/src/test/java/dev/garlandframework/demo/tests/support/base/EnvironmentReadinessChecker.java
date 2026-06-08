package dev.garlandframework.demo.tests.support.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.garlandframework.base.Pipeline;
import dev.garlandframework.base.checks.Verify;
import dev.garlandframework.base.retry.RetryConfig;
import dev.garlandframework.http.HttpTestClient;
import dev.garlandframework.http.model.HttpCallRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class EnvironmentReadinessChecker {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentReadinessChecker.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record HealthDto(String status) {}

    public void waitForServicesHealthy() {
        log.info("=== Stage 1: waiting for service health endpoints ===");
        checkHealth("user-service",   Connections.USER_SERVICE_URL        + "/actuator/health");
        checkHealth("projection-service", Connections.PROJECTION_SERVICE_URL + "/actuator/health");
        log.info("=== Stage 1 passed: both services are UP ===");
    }

    private void checkHealth(String name, String url) {
        log.info("  Polling {} at {} ...", name, url);
        HttpTestClient probe = new HttpTestClient(RetryConfig.of(60, Duration.ofSeconds(2)));
        Pipeline.given(HttpCallRequest.get(url))
                .then(probe.makeCall(200, HealthDto.class))
                .then(Verify.matching(new HealthDto("UP")))
                .execute();
        log.info("  {} is UP", name);
    }
}
