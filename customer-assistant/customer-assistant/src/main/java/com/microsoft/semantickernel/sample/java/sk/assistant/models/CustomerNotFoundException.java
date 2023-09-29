package com.microsoft.semantickernel.sample.java.sk.assistant.models;

public class CustomerNotFoundException extends Exception {
    public CustomerNotFoundException(String customerId) {
        super("Customer with id " + customerId + " not found");
    }
}
