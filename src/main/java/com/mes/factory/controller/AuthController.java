package com.mes.factory.controller;

import com.mes.factory.model.Employee;
import com.mes.factory.repository.EmployeeRepository;
import com.mes.factory.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        String password = body.get("password");
        
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee ID is required"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
        }

        Optional<Employee> empOpt = employeeRepository.findById(employeeId);
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();
            // Check password match (simple check, or default to ID if not set, but seeded is password123)
            String expectedPassword = emp.getPassword() != null ? emp.getPassword() : emp.getId();
            if (expectedPassword.equals(password)) {
                auditLogService.log(emp.getName(), "User Login", "Logged in with role: " + emp.getRole());
                return ResponseEntity.ok(emp);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid password"));
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Employee ID not found"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Employee employee) {
        if (employee.getId() == null || employee.getId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee ID is required"));
        }
        if (employee.getName() == null || employee.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
        }
        if (employee.getRole() == null || employee.getRole().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role is required"));
        }
        if (employee.getPassword() == null || employee.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
        }
        if (employeeRepository.existsById(employee.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Employee ID already exists"));
        }
        Employee saved = employeeRepository.save(employee);
        auditLogService.log(saved.getName(), "Register Personnel", "Registered as role: " + saved.getRole());
        return ResponseEntity.ok(saved);
    }
}
