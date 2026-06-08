package dev.garlandframework.demo.projectionservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.garlandframework.demo.projectionservice.event.UserCreatedEvent;
import dev.garlandframework.demo.projectionservice.event.UserDeletedEvent;
import dev.garlandframework.demo.projectionservice.event.UserUpdatedEvent;
import dev.garlandframework.demo.projectionservice.service.ProjectionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final ProjectionService projectionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user.created", groupId = "projection-service")
    public void onUserCreated(String message) {
        try {
            UserCreatedEvent event = objectMapper.readValue(message, UserCreatedEvent.class);
            projectionService.upsert(event);
            log.info("Processed user.created for userId={}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process user.created message: {}", message, e);
        }
    }

    @KafkaListener(topics = "user.updated", groupId = "projection-service")
    public void onUserUpdated(String message) {
        try {
            UserUpdatedEvent event = objectMapper.readValue(message, UserUpdatedEvent.class);
            projectionService.upsert(event);
            log.info("Processed user.updated for userId={}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process user.updated message: {}", message, e);
        }
    }

    @KafkaListener(topics = "user.deleted", groupId = "projection-service")
    public void onUserDeleted(String message) {
        try {
            UserDeletedEvent event = objectMapper.readValue(message, UserDeletedEvent.class);
            projectionService.delete(event.userId());
            log.info("Processed user.deleted for userId={}", event.userId());
        } catch (Exception e) {
            log.error("Failed to process user.deleted message: {}", message, e);
        }
    }
}
