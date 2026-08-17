package com.mes.factory.repository;

import com.mes.factory.model.Machine;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineRepository extends MongoRepository<Machine, String> {
}
