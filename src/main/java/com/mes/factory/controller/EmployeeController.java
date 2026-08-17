package com.mes.factory.controller;

import com.mes.factory.model.Employee;
import com.mes.factory.repository.EmployeeRepository;
import com.mes.factory.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    @GetMapping("/role/{role}")
    public List<Employee> getByRole(@PathVariable String role) {
        return employeeRepository.findByRole(role);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getById(@PathVariable String id) {
        Optional<Employee> empOpt = employeeRepository.findById(id);
        return empOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Employee create(@RequestBody Employee employee,
            @RequestHeader(value = "X-Username", required = false) String username) {
        Employee saved = employeeRepository.save(employee);
        auditLogService.log(username, "Create Employee",
                "Created employee: " + saved.getId() + " - " + saved.getName() + " (" + saved.getRole() + ")");
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable String id, @RequestBody Employee employee,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        employee.setId(id);
        Employee saved = employeeRepository.save(employee);
        auditLogService.log(username, "Update Employee",
                "Updated employee: " + saved.getId() + " - " + saved.getName() + " (" + saved.getRole() + ")");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        employeeRepository.deleteById(id);
        auditLogService.log(username, "Delete Employee", "Deleted employee: " + id);
        return ResponseEntity.ok().build();
    }
}
