package com.mes.factory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "machines")
public class Machine {
    @Id
    private String id; // e.g. "M101", "M102"
    private String name;
    private String status; // "Running", "Idle", "Maintenance"
    private String location;
}
