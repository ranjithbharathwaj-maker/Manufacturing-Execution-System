package com.mes.factory.controller;

import com.mes.factory.model.Machine;
import com.mes.factory.model.Maintenance;
import com.mes.factory.repository.MachineRepository;
import com.mes.factory.repository.MaintenanceRepository;
import com.mes.factory.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/maintenance")
@CrossOrigin(origins = "*")
public class MaintenanceController {

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public List<Maintenance> getAll() {
        return maintenanceRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody Maintenance ticket,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (ticket.getMachineId() == null || ticket.getProblem() == null) {
            return ResponseEntity.badRequest().body("machineId and problem are required");
        }

        Optional<Machine> machOpt = machineRepository.findById(ticket.getMachineId());
        if (machOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Machine not found: " + ticket.getMachineId());
        }

        ticket.setStatus("Pending");
        ticket.setDate(new Date());
        Maintenance saved = maintenanceRepository.save(ticket);

        // Put machine into Maintenance status
        Machine machine = machOpt.get();
        machine.setStatus("Maintenance");
        machineRepository.save(machine);

        auditLogService.log(username, "Log Maintenance Ticket",
                "Logged maintenance for machine: " + ticket.getMachineId() + ", problem: " + ticket.getProblem());

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTicketStatus(@PathVariable String id, @RequestBody Maintenance ticketUpdate,
            @RequestHeader(value = "X-Username", required = false) String username) {
        Optional<Maintenance> ticketOpt = maintenanceRepository.findById(id);
        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Maintenance ticket = ticketOpt.get();
        String oldStatus = ticket.getStatus();

        if (ticketUpdate.getStatus() != null) {
            ticket.setStatus(ticketUpdate.getStatus());
        }
        if (ticketUpdate.getEngineerName() != null) {
            ticket.setEngineerName(ticketUpdate.getEngineerName());
        }

        Maintenance saved = maintenanceRepository.save(ticket);

        // If status becomes Resolved, set machine status back to Idle
        if ("Resolved".equalsIgnoreCase(ticket.getStatus()) && !"Resolved".equalsIgnoreCase(oldStatus)) {
            Optional<Machine> machOpt = machineRepository.findById(ticket.getMachineId());
            if (machOpt.isPresent()) {
                Machine machine = machOpt.get();
                if ("Maintenance".equalsIgnoreCase(machine.getStatus())) {
                    machine.setStatus("Idle");
                    machineRepository.save(machine);
                }
            }
            auditLogService.log(username, "Resolve Maintenance Ticket",
                    "Resolved maintenance ticket " + id + " for machine " + ticket.getMachineId() + " by "
                            + ticket.getEngineerName());
        } else {
            auditLogService.log(username, "Update Maintenance Ticket",
                    "Updated ticket " + id + " status to " + ticket.getStatus());
        }

        return ResponseEntity.ok(saved);
    }
}
