package org.mtodemo.projectionservice.controller;

import lombok.RequiredArgsConstructor;
import org.mtodemo.projectionservice.document.OrderProjectionDocument;
import org.mtodemo.projectionservice.document.UserProjectionDocument;
import org.mtodemo.projectionservice.service.OrderProjectionService;
import org.mtodemo.projectionservice.service.ProjectionService;
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
