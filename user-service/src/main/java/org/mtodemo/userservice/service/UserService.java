package org.mtodemo.userservice.service;

import lombok.RequiredArgsConstructor;
import org.mtodemo.userservice.dto.UserDto;
import org.mtodemo.userservice.dto.UserRequest;
import org.mtodemo.userservice.entity.CarEntity;
import org.mtodemo.userservice.entity.UserEntity;
import org.mtodemo.userservice.event.UserDeletedEvent;
import org.mtodemo.userservice.kafka.UserEventPublisher;
import org.mtodemo.userservice.mapper.EventMapper;
import org.mtodemo.userservice.mapper.UserMapper;
import org.mtodemo.userservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EventMapper eventMapper;
    private final UserEventPublisher eventPublisher;

    @Transactional
    public UserDto create(UserRequest request) {
        UserEntity entity = userMapper.toEntity(request);
        UserEntity saved = userRepository.saveAndFlush(entity);
        eventPublisher.publishCreated(eventMapper.toCreatedEvent(saved));
        return userMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public UserDto findById(UUID id) {
        return userMapper.toDto(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional
    public UserDto update(UUID id, UserRequest request) {
        UserEntity existing = getOrThrow(id);
        existing.setName(request.name());
        existing.setSurname(request.surname());
        existing.setAddress(userMapper.toAddressEntity(request.address()));
        List<CarEntity> updatedCars = request.cars() == null ? List.of() :
                request.cars().stream().map(userMapper::toCarEntity).toList();
        existing.getCars().clear();
        existing.getCars().addAll(updatedCars);
        UserEntity saved = userRepository.saveAndFlush(existing);
        eventPublisher.publishUpdated(eventMapper.toUpdatedEvent(saved));
        return userMapper.toDto(saved);
    }

    @Transactional
    public void delete(UUID id) {
        UserEntity entity = getOrThrow(id);
        userRepository.delete(entity);
        eventPublisher.publishDeleted(new UserDeletedEvent(entity.getId(), Instant.now()));
    }

    private UserEntity getOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + id));
    }
}
