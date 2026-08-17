package com.mes.factory.config;

import com.mes.factory.model.Employee;
import com.mes.factory.model.Machine;
import com.mes.factory.model.Product;
import com.mes.factory.model.RawMaterial;
import com.mes.factory.repository.EmployeeRepository;
import com.mes.factory.repository.MachineRepository;
import com.mes.factory.repository.ProductRepository;
import com.mes.factory.repository.RawMaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RawMaterialRepository rawMaterialRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Override
    public void run(String... args) throws Exception {
        // Pre-populate Employees
        if (employeeRepository.count() == 0) {
            employeeRepository.save(new Employee("E101", "Rahul", "OPERATOR", "Production", "password123"));
            employeeRepository.save(new Employee("E102", "Anjali", "INSPECTOR", "Quality", "password123"));
            employeeRepository.save(new Employee("E103", "Arjun", "OPERATOR", "Maintenance", "password123"));
            employeeRepository.save(new Employee("E104", "John (Manager)", "MANAGER", "Production", "password123"));
            employeeRepository.save(new Employee("E105", "Admin User", "ADMIN", "Management", "password123"));
            System.out.println("Pre-populated sample employees.");
        }

        // Pre-populate Products
        if (productRepository.count() == 0) {
            productRepository.save(new Product("P101", "Water Bottle", "Bottle", 15.0));
            productRepository.save(new Product("P102", "Plastic Cap", "Cap", 2.0));
            productRepository.save(new Product("P103", "Juice Bottle", "Bottle", 20.0));
            System.out.println("Pre-populated sample products.");
        }

        // Pre-populate Raw Materials
        if (rawMaterialRepository.count() == 0) {
            rawMaterialRepository.save(new RawMaterial("RM001", "Plastic Granules", 5000.0, "kg"));
            rawMaterialRepository.save(new RawMaterial("RM002", "Labels", 20000.0, "pcs"));
            rawMaterialRepository.save(new RawMaterial("RM003", "Bottle Caps", 25000.0, "pcs"));
            System.out.println("Pre-populated sample raw materials.");
        }

        // Pre-populate Machines
        if (machineRepository.count() == 0) {
            machineRepository.save(new Machine("M101", "Injection Machine", "Running", "Bay-A"));
            machineRepository.save(new Machine("M102", "Packaging Machine", "Idle", "Bay-B"));
            machineRepository.save(new Machine("M103", "Label Machine", "Maintenance", "Bay-C"));
            System.out.println("Pre-populated sample machines.");
        }
    }
}
