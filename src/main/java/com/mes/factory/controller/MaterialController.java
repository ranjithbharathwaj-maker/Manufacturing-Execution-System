package com.mes.factory.controller;

import com.mes.factory.model.RawMaterial;
import com.mes.factory.repository.RawMaterialRepository;
import com.mes.factory.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
public class MaterialController {

    @Autowired
    private RawMaterialRepository rawMaterialRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public List<RawMaterial> getAll() {
        return rawMaterialRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RawMaterial> getById(@PathVariable String id) {
        Optional<RawMaterial> matOpt = rawMaterialRepository.findById(id);
        return matOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public RawMaterial create(@RequestBody RawMaterial material,
            @RequestHeader(value = "X-Username", required = false) String username) {
        RawMaterial saved = rawMaterialRepository.save(material);
        auditLogService.log(username, "Create Raw Material", "Created raw material: " + saved.getId() + " - "
                + saved.getName() + " (" + saved.getQuantity() + " " + saved.getUnit() + ")");
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<RawMaterial> update(@PathVariable String id, @RequestBody RawMaterial material,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (!rawMaterialRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        material.setId(id);
        RawMaterial saved = rawMaterialRepository.save(material);
        auditLogService.log(username, "Update Raw Material", "Updated raw material: " + saved.getId() + " - "
                + saved.getName() + " (" + saved.getQuantity() + " " + saved.getUnit() + ")");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (!rawMaterialRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        rawMaterialRepository.deleteById(id);
        auditLogService.log(username, "Delete Raw Material", "Deleted material: " + id);
        return ResponseEntity.ok().build();
    }
}
