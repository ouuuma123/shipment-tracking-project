package com.example.Shipment.shipment.service;

import com.example.Shipment.shipment.entitie.ShipmentStatus;

public class InvalidShipmentTransitionException extends RuntimeException {

    public InvalidShipmentTransitionException(ShipmentStatus from, ShipmentStatus to) {
        super("Cannot change status from " + from + " to " + to + ".");
    }
}
