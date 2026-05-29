package org.mtodemo.projectionservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mtodemo.projectionservice.event.OrderCancelledEvent;
import org.mtodemo.projectionservice.event.OrderPlacedEvent;
import org.mtodemo.projectionservice.service.OrderProjectionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderProjectionService orderProjectionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.placed", groupId = "projection-service")
    public void onOrderPlaced(String message) {
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            orderProjectionService.project(event);
            log.info("Processed order.placed for orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process order.placed message: {}", message, e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "projection-service")
    public void onOrderCancelled(String message) {
        try {
            OrderCancelledEvent event = objectMapper.readValue(message, OrderCancelledEvent.class);
            orderProjectionService.cancel(event);
            log.info("Processed order.cancelled for orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to process order.cancelled message: {}", message, e);
        }
    }
}
