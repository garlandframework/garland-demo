package dev.garlandframework.demo.userservice.kafka;

import lombok.RequiredArgsConstructor;
import dev.garlandframework.demo.userservice.event.UserCreatedEvent;
import dev.garlandframework.demo.userservice.event.UserDeletedEvent;
import dev.garlandframework.demo.userservice.event.UserUpdatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {

    private static final String TOPIC_CREATED = "user.created";
    private static final String TOPIC_UPDATED = "user.updated";
    private static final String TOPIC_DELETED = "user.deleted";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCreated(UserCreatedEvent event) {
        kafkaTemplate.send(TOPIC_CREATED, event.userId().toString(), event);
    }

    public void publishUpdated(UserUpdatedEvent event) {
        kafkaTemplate.send(TOPIC_UPDATED, event.userId().toString(), event);
    }

    public void publishDeleted(UserDeletedEvent event) {
        kafkaTemplate.send(TOPIC_DELETED, event.userId().toString(), event);
    }
}
