package org.mtodemo.projectionservice.service;

import lombok.RequiredArgsConstructor;
import org.mtodemo.projectionservice.document.OrderProjectionDocument;
import org.mtodemo.projectionservice.event.OrderCancelledEvent;
import org.mtodemo.projectionservice.event.OrderPlacedEvent;
import org.mtodemo.projectionservice.mapper.OrderProjectionMapper;
import org.mtodemo.projectionservice.repository.OrderProjectionRepository;
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
