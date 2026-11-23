package com.example.Shipment.shipment.service;

import com.example.Shipment.shipment.entitie.ShipmentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShipmentStatusPolicyTest {

    @Test
    void allowsTheForwardPathAndItsBranches() {
        assertDoesNotThrow(() -> ShipmentStatusPolicy.requireTransition(
                ShipmentStatus.CREATED, ShipmentStatus.PICKED_UP));
        assertDoesNotThrow(() -> ShipmentStatusPolicy.requireTransition(
                ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT));
        assertDoesNotThrow(() -> ShipmentStatusPolicy.requireTransition(
                ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY));
        assertDoesNotThrow(() -> ShipmentStatusPolicy.requireTransition(
                ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED));
        assertDoesNotThrow(() -> ShipmentStatusPolicy.requireTransition(
                ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.FAILED_DELIVERY));
        assertDoesNotThrow(() -> ShipmentStatusPolicy.requireTransition(
                ShipmentStatus.FAILED_DELIVERY, ShipmentStatus.RETURNED));
    }

    @Test
    void rejectsSkippingOrLeavingAFinalStatus() {
        assertThrows(InvalidShipmentTransitionException.class, () -> ShipmentStatusPolicy.requireTransition(
                ShipmentStatus.CREATED, ShipmentStatus.DELIVERED));
        assertThrows(InvalidShipmentTransitionException.class, () -> ShipmentStatusPolicy.requireTransition(
                ShipmentStatus.DELIVERED, ShipmentStatus.CREATED));
        assertThrows(InvalidShipmentTransitionException.class, () -> ShipmentStatusPolicy.requireTransition(
                ShipmentStatus.CANCELLED, ShipmentStatus.PICKED_UP));
    }
}
