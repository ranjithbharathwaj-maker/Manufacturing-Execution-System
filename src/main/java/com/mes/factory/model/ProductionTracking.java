package com.mes.factory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "production_tracking")
public class ProductionTracking {
    @Id
    private String id;
    private String orderId;
    private String machineId;
    private String employeeId;
    private int completedQuantity;
    private int defectiveQuantity;

    // Defect reasons map, e.g. {"Crack": 30, "Color mismatch": 10}
    private Map<String, Integer> defectReasons;
}
