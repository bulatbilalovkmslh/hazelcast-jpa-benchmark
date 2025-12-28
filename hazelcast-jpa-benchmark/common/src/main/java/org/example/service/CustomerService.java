package org.example.service;

import org.example.entity.Customer;

import java.util.Optional;

public interface CustomerService {
    public Optional<Customer> findById(Long id);
    public Customer create(Customer customer);
    public Customer updateEmail(Long id, String newEmail);

    void evictSecondLevelCacheEntry(Long id);
    void evictAllCustomersFromSecondLevelCache();
}
