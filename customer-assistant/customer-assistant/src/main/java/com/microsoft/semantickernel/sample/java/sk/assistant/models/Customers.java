package com.microsoft.semantickernel.sample.java.sk.assistant.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Customers {
    private final Map<String, Customer> customers;

    public Customers() {
        customers = new HashMap<>();
    }

    public Customers(Map<String, Customer> customers) {
        this.customers = new HashMap<>(customers);
    }

    public Customer getCustomer(String customerId) throws CustomerNotFoundException {
        Customer customer = customers.get(customerId);
        if (customer == null) {
            throw new CustomerNotFoundException(customerId);
        }
        return customer;
    }

    public List<String> getCustomerNames() {
        return new ArrayList<>(customers.keySet());
    }

    public Customers addCustomer(Customer customer) {
        customers.put(customer.getUid(), customer);
        return this;
    }

    public Collection<Customer> getCustomers() {
        return customers.values();
    }
}
