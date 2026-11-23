package com.example.Shipment.user.service;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("An account already exists for " + email);
    }
}
