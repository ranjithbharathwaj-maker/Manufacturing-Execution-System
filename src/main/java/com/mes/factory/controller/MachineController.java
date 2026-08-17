package com.mes.factory.controller;

import com.mes.factory.model.Machine;
import com.mes.factory.repository.MachineRepository;
import com.mes.factory.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/machines")
@CrossOrigin(origins = "*")
public class MachineController {

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public List<Machine> getAll() {
        return machineRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Machine> getById(@PathVariable String id) {
        Optional<Machine> machOpt = machineRepository.findById(id);
        return machOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Machine create(@RequestBody Machine machine,
            @RequestHeader(value = "X-Username", required = false) String username) {
        Machine saved = machineRepository.save(machine);
        auditLogService.log(username, "Create Machine",
                "Created machine: " + saved.getId() + " - " + saved.getName() + " (" + saved.getStatus() + ")");
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Machine> update(@PathVariable String id, @RequestBody Machine machine,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (!machineRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        machine.setId(id);
        Machine saved = machineRepository.save(machine);
        auditLogService.log(username, "Update Machine",
                "Updated machine: " + saved.getId() + " - " + saved.getName() + " (" + saved.getStatus() + ")");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (!machineRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        machineRepository.deleteById(id);
        auditLogService.log(username, "Delete Machine", "Deleted machine: " + id);
        return ResponseEntity.ok().build();
    }
}
