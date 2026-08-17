package com.mes.factory.repository;

import com.mes.factory.model.Maintenance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRepository extends MongoRepository<Maintenance, String> {
    List<Maintenance> findByMachineId(String machineId);
}
