package dev.garlandframework.demo.projectionservice.service;

import lombok.RequiredArgsConstructor;
import dev.garlandframework.demo.projectionservice.document.UserProjectionDocument;
import dev.garlandframework.demo.projectionservice.event.UserCreatedEvent;
import dev.garlandframework.demo.projectionservice.event.UserUpdatedEvent;
import dev.garlandframework.demo.projectionservice.mapper.ProjectionMapper;
import dev.garlandframework.demo.projectionservice.repository.UserProjectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectionService {

    private final UserProjectionRepository repository;
    private final ProjectionMapper projectionMapper;

    public void upsert(UserCreatedEvent event) {
        UserProjectionDocument document = projectionMapper.toDocument(event);
        repository.save(document);
    }

    public void upsert(UserUpdatedEvent event) {
        UserProjectionDocument document = projectionMapper.toDocument(event);
        repository.save(document);
    }

    public void delete(UUID userId) {
        repository.deleteById(userId);
    }

    public UserProjectionDocument findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Projection not found: " + id));
    }

    public List<UserProjectionDocument> findAll() {
        return repository.findAll();
    }
}
