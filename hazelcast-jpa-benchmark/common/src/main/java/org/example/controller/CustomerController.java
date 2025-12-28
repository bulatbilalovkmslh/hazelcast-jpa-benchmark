package org.example.controller;

import jakarta.persistence.EntityManagerFactory;
import org.example.dto.CustomerResponse;
import org.example.dto.UpdateEmailRequest;
import org.example.entity.Customer;
import org.example.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;
    private final EntityManagerFactory emf;

    @Value("${app.node-id:unknown}")
    private String nodeId;

    public CustomerController(CustomerService customerService, EntityManagerFactory emf) {
        this.customerService = customerService;
        this.emf = emf;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        boolean inL2Before = emf.getCache().contains(Customer.class, id);
        String cacheStatus = inL2Before ? "cache-hit" : "cache-miss";

        Optional<Customer> customerOpt = customerService.findById(id);
        if (customerOpt.isEmpty()) {
            log.info("[{}] {} id={}: not-found", nodeId, cacheStatus, id);
            return ResponseEntity.notFound().build();
        }

        Customer c = customerOpt.get();

        log.info("[{}] {} id={}", nodeId, cacheStatus, id);
        return ResponseEntity.ok(new CustomerResponse(
                c.getId(),
                c.getName(),
                c.getEmail(),
                cacheStatus,
                nodeId
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomerEmail(
            @PathVariable Long id,
            @RequestBody UpdateEmailRequest request) {
        Customer updated = customerService.updateEmail(id, request.email());

        log.info("[{}] cache-evict id={}", nodeId, id);

        return ResponseEntity.ok(new CustomerResponse(
                updated.getId(),
                updated.getName(),
                updated.getEmail(),
                "updated",
                nodeId
        ));
    }
}