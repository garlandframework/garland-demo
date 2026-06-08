package dev.garlandframework.demo.projectionservice.service;

import lombok.RequiredArgsConstructor;
import dev.garlandframework.demo.projectionservice.document.OrderProjectionDocument;
import dev.garlandframework.demo.projectionservice.event.OrderCancelledEvent;
import dev.garlandframework.demo.projectionservice.event.OrderPlacedEvent;
import dev.garlandframework.demo.projectionservice.mapper.OrderProjectionMapper;
import dev.garlandframework.demo.projectionservice.repository.OrderProjectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderProjectionService {

    private final OrderProjectionRepository repository;
    private final OrderProjectionMapper mapper;

    public void project(OrderPlacedEvent event) {
        repository.save(mapper.toDocument(event));
    }

    public void cancel(OrderCancelledEvent event) {
        OrderProjectionDocument doc = repository.findById(event.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order projection not found: " + event.orderId()));
        doc.setStatus("CANCELLED");
        repository.save(doc);
    }

    public OrderProjectionDocument findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order projection not found: " + id));
    }
}
