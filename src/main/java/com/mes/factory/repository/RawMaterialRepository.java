package com.mes.factory.repository;

import com.mes.factory.model.RawMaterial;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawMaterialRepository extends MongoRepository<RawMaterial, String> {
}
