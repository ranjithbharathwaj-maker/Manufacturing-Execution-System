package com.mes.factory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "raw_materials")
public class RawMaterial {
    @Id
    private String id; // This can hold "RM101", "RM102", "material_name_slug"
    private String name;
    private double quantity;
    private String unit;
}
