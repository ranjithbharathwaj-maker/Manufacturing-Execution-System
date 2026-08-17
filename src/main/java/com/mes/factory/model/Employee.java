package com.mes.factory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "employees")
public class Employee {
    @Id
    private String id; // e.g. "Rahul", "Anjali" or E101
    private String name;
    private String role; // "ADMIN", "MANAGER", "OPERATOR", "INSPECTOR"
    private String department; // "Production", "Quality", "Maintenance"
    private String password; // login password
}
