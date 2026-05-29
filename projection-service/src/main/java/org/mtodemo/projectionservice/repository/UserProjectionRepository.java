package org.mtodemo.projectionservice.repository;

import org.mtodemo.projectionservice.document.UserProjectionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserProjectionRepository extends MongoRepository<UserProjectionDocument, UUID> {
}
