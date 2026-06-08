package dev.garlandframework.demo.projectionservice.repository;

import dev.garlandframework.demo.projectionservice.document.OrderProjectionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderProjectionRepository extends MongoRepository<OrderProjectionDocument, UUID> {
}
