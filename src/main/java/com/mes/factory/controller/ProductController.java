package com.mes.factory.controller;

import com.mes.factory.model.Product;
import com.mes.factory.repository.ProductRepository;
import com.mes.factory.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable String id) {
        Optional<Product> prodOpt = productRepository.findById(id);
        return prodOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product create(@RequestBody Product product,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (product.getQuantity() < 0) {
            product.setQuantity(0);
        }
        Product saved = productRepository.save(product);
        auditLogService.log(username, "Create Product", "Created product: " + saved.getId() + " - " + saved.getName());
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable String id, @RequestBody Product product,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        product.setId(id);
        Product saved = productRepository.save(product);
        auditLogService.log(username, "Update Product", "Updated product: " + saved.getId() + " - " + saved.getName());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
            @RequestHeader(value = "X-Username", required = false) String username) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        auditLogService.log(username, "Delete Product", "Deleted product: " + id);
        return ResponseEntity.ok().build();
    }
}
