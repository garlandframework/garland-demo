package org.mtodemo.userservice.kafka;

import lombok.RequiredArgsConstructor;
import org.mtodemo.userservice.event.OrderCancelledEvent;
import org.mtodemo.userservice.event.OrderPlacedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final String TOPIC_PLACED = "order.placed";
    private static final String TOPIC_CANCELLED = "order.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send(TOPIC_PLACED, event.orderId().toString(), event);
    }

    public void publishCancelled(OrderCancelledEvent event) {
        kafkaTemplate.send(TOPIC_CANCELLED, event.orderId().toString(), event);
    }
}
