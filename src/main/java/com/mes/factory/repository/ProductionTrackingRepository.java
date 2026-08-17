package com.mes.factory.repository;

import com.mes.factory.model.ProductionTracking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductionTrackingRepository extends MongoRepository<ProductionTracking, String> {
    Optional<ProductionTracking> findByOrderId(String orderId);
}
