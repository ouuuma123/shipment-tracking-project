package com.example.Shipment.user.service;

public class InvalidAuthTokenException extends RuntimeException {
    public InvalidAuthTokenException() {
        super("The supplied authentication token is invalid or expired.");
    }
}
