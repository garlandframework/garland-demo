package org.mtodemo.userservice.service;

import lombok.RequiredArgsConstructor;
import org.mtodemo.userservice.dto.OrderDto;
import org.mtodemo.userservice.dto.OrderRequest;
import org.mtodemo.userservice.dto.OrderStatus;
import org.mtodemo.userservice.entity.OrderEntity;
import org.mtodemo.userservice.kafka.OrderEventPublisher;
import org.mtodemo.userservice.mapper.OrderEventMapper;
import org.mtodemo.userservice.mapper.OrderMapper;
import org.mtodemo.userservice.repository.OrderRepository;
import org.mtodemo.userservice.repository.UserRepository;
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
