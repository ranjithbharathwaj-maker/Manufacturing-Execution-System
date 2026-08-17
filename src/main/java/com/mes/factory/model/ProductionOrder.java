package com.mes.factory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "production_orders")
public class ProductionOrder {
    @Id
    private String id; // PO001
    private String productId;
    private int quantity;
    private Date startDate;
    private Date endDate;
    private String status; // "Pending", "Running", "Completed", "Cancelled"

    // Allocations
    private String machineId;
    private String operatorId;
    private String shift; // "Morning", "Afternoon", "Night"
}
