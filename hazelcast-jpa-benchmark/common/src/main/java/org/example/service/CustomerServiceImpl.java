package org.example.service;

import jakarta.persistence.EntityManagerFactory;
import org.example.entity.Customer;
import org.example.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final EntityManagerFactory entityManagerFactory;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                           EntityManagerFactory entityManagerFactory) {
        this.customerRepository = customerRepository;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Transactional
    public Customer create(Customer customer) {
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateEmail(Long id, String newEmail) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        customer.setEmail(newEmail);

        Customer saved = customerRepository.save(customer);

        evictSecondLevelCacheEntry(id);

        return saved;
    }

    public void evictSecondLevelCacheEntry(Long id) {
        entityManagerFactory.getCache().evict(Customer.class, id);
    }

    public void evictAllCustomersFromSecondLevelCache() {
        entityManagerFactory.getCache().evict(Customer.class);
    }
}
