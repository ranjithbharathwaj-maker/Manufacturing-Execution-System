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
@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    private String username;
    private String action;
    private Date timestamp;
    private String details;
}
