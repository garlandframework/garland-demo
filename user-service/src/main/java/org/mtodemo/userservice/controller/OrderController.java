package org.mtodemo.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mtodemo.userservice.dto.OrderDto;
import org.mtodemo.userservice.dto.OrderRequest;
import org.mtodemo.userservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDto> place(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.place(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(orderService.findByUserId(userId));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderDto> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}
