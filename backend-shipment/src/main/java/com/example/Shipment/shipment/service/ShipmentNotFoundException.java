package com.example.Shipment.shipment.service;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException() {
        super("Shipment not found.");
    }
}
