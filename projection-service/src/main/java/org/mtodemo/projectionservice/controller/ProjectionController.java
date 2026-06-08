package dev.garlandframework.demo.projectionservice.controller;

import lombok.RequiredArgsConstructor;
import dev.garlandframework.demo.projectionservice.document.OrderProjectionDocument;
import dev.garlandframework.demo.projectionservice.document.UserProjectionDocument;
import dev.garlandframework.demo.projectionservice.service.OrderProjectionService;
import dev.garlandframework.demo.projectionservice.service.ProjectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projections")
@RequiredArgsConstructor
public class ProjectionController {

    private final ProjectionService projectionService;
    private final OrderProjectionService orderProjectionService;

    @GetMapping("/users/{id}")
    public ResponseEntity<UserProjectionDocument> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(projectionService.findById(id));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderProjectionDocument> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderProjectionService.findById(id));
    }
}
