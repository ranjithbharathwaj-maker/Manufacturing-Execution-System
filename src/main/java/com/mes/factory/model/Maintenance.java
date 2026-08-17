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
@Document(collection = "maintenance_tickets")
public class Maintenance {
    @Id
    private String id;
    private String machineId;
    private String problem;
    private String status; // "Pending", "In Progress", "Resolved"
    private Date date;
    private String engineerName;
}
