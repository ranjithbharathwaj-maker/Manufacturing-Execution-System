package com.mes.factory.repository;

import com.mes.factory.model.ProductionOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionOrderRepository extends MongoRepository<ProductionOrder, String> {
    List<ProductionOrder> findByStatus(String status);
}
