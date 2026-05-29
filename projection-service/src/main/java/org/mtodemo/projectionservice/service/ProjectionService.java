package org.mtodemo.projectionservice.service;

import lombok.RequiredArgsConstructor;
import org.mtodemo.projectionservice.document.UserProjectionDocument;
import org.mtodemo.projectionservice.event.UserCreatedEvent;
import org.mtodemo.projectionservice.event.UserUpdatedEvent;
import org.mtodemo.projectionservice.mapper.ProjectionMapper;
import org.mtodemo.projectionservice.repository.UserProjectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
