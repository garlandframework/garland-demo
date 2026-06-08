package dev.garlandframework.demo.userservice.service;

import lombok.RequiredArgsConstructor;
import dev.garlandframework.demo.userservice.dto.OrderDto;
import dev.garlandframework.demo.userservice.dto.OrderRequest;
import dev.garlandframework.demo.userservice.dto.OrderStatus;
import dev.garlandframework.demo.userservice.entity.OrderEntity;
import dev.garlandframework.demo.userservice.kafka.OrderEventPublisher;
import dev.garlandframework.demo.userservice.mapper.OrderEventMapper;
import dev.garlandframework.demo.userservice.mapper.OrderMapper;
import dev.garlandframework.demo.userservice.repository.OrderRepository;
import dev.garlandframework.demo.userservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderEventMapper orderEventMapper;
    private final OrderEventPublisher orderEventPublisher;

    @Transactional
    public OrderDto place(OrderRequest request) {
        if (!userRepository.existsById(request.userId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User not found: " + request.userId());
        }
        OrderEntity entity = orderMapper.toEntity(request);
        entity.setStatus(OrderStatus.PENDING);
        OrderEntity saved = orderRepository.saveAndFlush(entity);
        orderEventPublisher.publishPlaced(orderEventMapper.toPlacedEvent(saved));
        return orderMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderDto findById(UUID id) {
        return orderMapper.toDto(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findByUserId(UUID userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Transactional
    public OrderDto cancel(UUID id) {
        OrderEntity order = getOrThrow(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order is already cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        OrderEntity saved = orderRepository.saveAndFlush(order);
        orderEventPublisher.publishCancelled(orderEventMapper.toCancelledEvent(saved));
        return orderMapper.toDto(saved);
    }

    private OrderEntity getOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found: " + id));
    }
}
