package com.mes.factory.controller;

import com.mes.factory.model.ProductionTracking;
import com.mes.factory.repository.ProductionTrackingRepository;
import com.mes.factory.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = "*")
public class TrackingController {

    @Autowired
    private ProductionTrackingRepository trackingRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public List<ProductionTracking> getAll() {
        return trackingRepository.findAll();
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ProductionTracking> getByOrderId(@PathVariable String orderId) {
        Optional<ProductionTracking> trackingOpt = trackingRepository.findByOrderId(orderId);
        return trackingOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/order/{orderId}")
    public ResponseEntity<?> updateTracking(@PathVariable String orderId, @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Username", required = false) String username) {
        Optional<ProductionTracking> trackingOpt = trackingRepository.findByOrderId(orderId);
        if (trackingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProductionTracking tracking = trackingOpt.get();
        if (body.containsKey("completedQuantity")) {
            tracking.setCompletedQuantity((Integer) body.get("completedQuantity"));
        }
        if (body.containsKey("defectiveQuantity")) {
            tracking.setDefectiveQuantity((Integer) body.get("defectiveQuantity"));
        }
        if (body.containsKey("defectReasons")) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> reasons = (Map<String, Integer>) body.get("defectReasons");
            tracking.setDefectReasons(reasons);
        }

        ProductionTracking saved = trackingRepository.save(tracking);
        auditLogService.log(username, "Update Production Tracking",
                "Updated tracking for Order: " + orderId + ". Good: " + tracking.getCompletedQuantity()
                        + ", Defective: " + tracking.getDefectiveQuantity());

        return ResponseEntity.ok(saved);
    }
}
