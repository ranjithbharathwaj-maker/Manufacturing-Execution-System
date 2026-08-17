package com.mes.factory.service;

import com.mes.factory.model.*;
import com.mes.factory.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

@Service
public class ProductionService {

    @Autowired
    private ProductionOrderRepository orderRepository;

    @Autowired
    private RawMaterialRepository materialRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProductionTrackingRepository trackingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditLogService auditLogService;

    // Defines how much material is consumed per product unit.
    // For example, P101 (Water Bottle) requires:
    // - Plastic Granules (RM001): 0.07 kg
    // - Labels (RM002): 1.0 pcs
    // - Bottle Caps (RM003): 1.0 pcs
    public boolean checkAndConsumeMaterials(String productId, int quantity, String username) {
        if ("P101".equals(productId) || "P103".equals(productId)) { // Bottles
            Optional<RawMaterial> plasticGranulesOpt = materialRepository.findById("RM001");
            Optional<RawMaterial> labelsOpt = materialRepository.findById("RM002");
            Optional<RawMaterial> capsOpt = materialRepository.findById("RM003");

            if (plasticGranulesOpt.isPresent() && labelsOpt.isPresent() && capsOpt.isPresent()) {
                RawMaterial plastic = plasticGranulesOpt.get();
                RawMaterial labels = labelsOpt.get();
                RawMaterial caps = capsOpt.get();

                double plasticNeeded = quantity * 0.07; // 0.07 kg per bottle
                double labelsNeeded = quantity * 1.0;
                double capsNeeded = quantity * 1.0;

                if (plastic.getQuantity() >= plasticNeeded &&
                        labels.getQuantity() >= labelsNeeded &&
                        caps.getQuantity() >= capsNeeded) {

                    // Deduct
                    plastic.setQuantity(plastic.getQuantity() - plasticNeeded);
                    labels.setQuantity(labels.getQuantity() - labelsNeeded);
                    caps.setQuantity(caps.getQuantity() - capsNeeded);

                    materialRepository.save(plastic);
                    materialRepository.save(labels);
                    materialRepository.save(caps);

                    auditLogService.log(username, "Inventory Decrement",
                            "Consumed for production order: " + plasticNeeded + " kg RM001, " +
                                    labelsNeeded + " RM002, " + capsNeeded + " RM003.");
                    return true;
                }
            }
        } else if ("P102".equals(productId)) { // Cap
            Optional<RawMaterial> plasticGranulesOpt = materialRepository.findById("RM001");
            if (plasticGranulesOpt.isPresent()) {
                RawMaterial plastic = plasticGranulesOpt.get();
                double plasticNeeded = quantity * 0.01; // 0.01 kg per cap
                if (plastic.getQuantity() >= plasticNeeded) {
                    plastic.setQuantity(plastic.getQuantity() - plasticNeeded);
                    materialRepository.save(plastic);
                    auditLogService.log(username, "Inventory Decrement", "Consumed: " + plasticNeeded + " kg RM001");
                    return true;
                }
            }
        } else {
            // Fallback for custom products
            auditLogService.log(username, "Inventory Check", "No default raw materials configured for product: " + productId + ". Proceeding without deduction.");
            return true;
        }
        return false;
    }

    public ProductionOrder startProduction(String orderId, String machineId, String operatorId, String shift,
            String username) {
        Optional<ProductionOrder> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("Production Order not found: " + orderId);
        }

        ProductionOrder order = orderOpt.get();
        if (!"Pending".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Only Pending orders can be started. Current status: " + order.getStatus());
        }

        // Validate Machine
        Optional<Machine> machOpt = machineRepository.findById(machineId);
        if (machOpt.isEmpty()) {
            throw new RuntimeException("Machine not found: " + machineId);
        }
        Machine machine = machOpt.get();
        if ("Maintenance".equalsIgnoreCase(machine.getStatus())) {
            throw new RuntimeException("Machine " + machineId + " is under Maintenance. Cannot allocate.");
        }

        // Validate Employee
        Optional<Employee> empOpt = employeeRepository.findById(operatorId);
        if (empOpt.isEmpty()) {
            throw new RuntimeException("Operator not found: " + operatorId);
        }

        // Check & Consume Materials
        boolean success = checkAndConsumeMaterials(order.getProductId(), order.getQuantity(), username);
        if (!success) {
            throw new RuntimeException(
                    "Insufficient inventory to start production of quantity: " + order.getQuantity());
        }

        // Update Machine status to Running
        machine.setStatus("Running");
        machineRepository.save(machine);

        // Update Order
        order.setMachineId(machineId);
        order.setOperatorId(operatorId);
        order.setShift(shift);
        order.setStartDate(new Date());
        order.setStatus("Running");
        ProductionOrder savedOrder = orderRepository.save(order);

        // Initialize Tracking row
        ProductionTracking tracking = new ProductionTracking();
        tracking.setOrderId(orderId);
        tracking.setMachineId(machineId);
        tracking.setEmployeeId(operatorId);
        tracking.setCompletedQuantity(0);
        tracking.setDefectiveQuantity(0);
        tracking.setDefectReasons(new HashMap<>());
        trackingRepository.save(tracking);

        auditLogService.log(username, "Start Production",
                "Started Order " + orderId + " using Machine " + machineId + " operated by " + operatorId);

        return savedOrder;
    }

    public ProductionOrder completeProduction(String orderId, String username) {
        Optional<ProductionOrder> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("Production Order not found: " + orderId);
        }

        ProductionOrder order = orderOpt.get();
        if (!"Running".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("Only Running orders can be completed.");
        }

        order.setStatus("Completed");
        order.setEndDate(new Date());
        ProductionOrder savedOrder = orderRepository.save(order);

        // Free up machine
        if (order.getMachineId() != null) {
            Optional<Machine> machOpt = machineRepository.findById(order.getMachineId());
            if (machOpt.isPresent()) {
                Machine machine = machOpt.get();
                if ("Running".equalsIgnoreCase(machine.getStatus())) {
                    machine.setStatus("Idle");
                    machineRepository.save(machine);
                }
            }
        }

        // Increase finished goods inventory stock
        Optional<ProductionTracking> trackingOpt = trackingRepository.findByOrderId(orderId);
        if (trackingOpt.isPresent()) {
            ProductionTracking tracking = trackingOpt.get();
            int goodQuantity = tracking.getCompletedQuantity(); // Succeeded units
            if (goodQuantity > 0) {
                Optional<Product> prodOpt = productRepository.findById(order.getProductId());
                if (prodOpt.isPresent()) {
                    Product prod = prodOpt.get();
                    prod.setQuantity(prod.getQuantity() + goodQuantity);
                    productRepository.save(prod);
                    auditLogService.log(username, "Inventory Increment",
                            "Added " + goodQuantity + " units to finished stock of " + prod.getName());
                }
            }
        }

        auditLogService.log(username, "Complete Production", "Completed Order " + orderId);
        return savedOrder;
    }

    public ProductionOrder cancelProduction(String orderId, String username) {
        Optional<ProductionOrder> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("Production Order not found: " + orderId);
        }

        ProductionOrder order = orderOpt.get();
        String prevStatus = order.getStatus();
        order.setStatus("Cancelled");
        ProductionOrder savedOrder = orderRepository.save(order);

        // Free up machine if running
        if ("Running".equalsIgnoreCase(prevStatus) && order.getMachineId() != null) {
            Optional<Machine> machOpt = machineRepository.findById(order.getMachineId());
            if (machOpt.isPresent()) {
                Machine machine = machOpt.get();
                if ("Running".equalsIgnoreCase(machine.getStatus())) {
                    machine.setStatus("Idle");
                    machineRepository.save(machine);
                }
            }
        }

        auditLogService.log(username, "Cancel Production", "Cancelled Order " + orderId);
        return savedOrder;
    }
}
