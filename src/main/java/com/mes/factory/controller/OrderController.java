package com.mes.factory.controller;

import com.mes.factory.model.ProductionOrder;
import com.mes.factory.repository.ProductionOrderRepository;
import com.mes.factory.service.ProductionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private ProductionOrderRepository orderRepository;

    @Autowired
    private ProductionService productionService;

    @GetMapping
    public List<ProductionOrder> getAll() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductionOrder> getById(@PathVariable String id) {
        Optional<ProductionOrder> orderOpt = orderRepository.findById(id);
        return orderOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ProductionOrder create(@RequestBody ProductionOrder order) {
        order.setStatus("Pending");
        return orderRepository.save(order);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startProduction(@PathVariable String id, @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Username", required = false) String username) {
        String machineId = body.get("machineId");
        String operatorId = body.get("operatorId");
        String shift = body.get("shift");

        if (machineId == null || operatorId == null || shift == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "machineId, operatorId, and shift are required"));
        }

        try {
            ProductionOrder startedOrder = productionService.startProduction(id, machineId, operatorId, shift,
                    username);
            return ResponseEntity.ok(startedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeProduction(@PathVariable String id,
            @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            ProductionOrder completedOrder = productionService.completeProduction(id, username);
            return ResponseEntity.ok(completedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelProduction(@PathVariable String id,
            @RequestHeader(value = "X-Username", required = false) String username) {
        try {
            ProductionOrder cancelledOrder = productionService.cancelProduction(id, username);
            return ResponseEntity.ok(cancelledOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
