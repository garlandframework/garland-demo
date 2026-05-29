package org.mtodemo.projectionservice.repository;

import org.mtodemo.projectionservice.document.OrderProjectionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderProjectionRepository extends MongoRepository<OrderProjectionDocument, UUID> {
}
