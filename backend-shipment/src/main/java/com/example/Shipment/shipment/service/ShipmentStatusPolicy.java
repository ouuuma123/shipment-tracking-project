package com.example.Shipment.shipment.service;

import com.example.Shipment.shipment.entitie.ShipmentStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ShipmentStatusPolicy {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED = new EnumMap<>(ShipmentStatus.class);

    static {
        ALLOWED.put(ShipmentStatus.CREATED, EnumSet.of(ShipmentStatus.PICKED_UP, ShipmentStatus.CANCELLED));
        ALLOWED.put(ShipmentStatus.PICKED_UP, EnumSet.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.CANCELLED));
        ALLOWED.put(ShipmentStatus.IN_TRANSIT, EnumSet.of(
                ShipmentStatus.OUT_FOR_DELIVERY,
                ShipmentStatus.FAILED_DELIVERY,
                ShipmentStatus.RETURNED,
                ShipmentStatus.CANCELLED));
        ALLOWED.put(ShipmentStatus.OUT_FOR_DELIVERY, EnumSet.of(
                ShipmentStatus.DELIVERED, ShipmentStatus.FAILED_DELIVERY));
        ALLOWED.put(ShipmentStatus.FAILED_DELIVERY, EnumSet.of(
                ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.RETURNED));
        ALLOWED.put(ShipmentStatus.DELIVERED, EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.RETURNED, EnumSet.noneOf(ShipmentStatus.class));
        ALLOWED.put(ShipmentStatus.CANCELLED, EnumSet.noneOf(ShipmentStatus.class));
    }

    private ShipmentStatusPolicy() {
    }

    public static boolean canTransition(ShipmentStatus from, ShipmentStatus to) {
        return from != null && (from == to || ALLOWED.getOrDefault(from, Set.of()).contains(to));
    }

    public static void requireTransition(ShipmentStatus from, ShipmentStatus to) {
        if (to == null) {
            throw new IllegalArgumentException("Status is required.");
        }
        if (!canTransition(from, to)) {
            throw new InvalidShipmentTransitionException(from, to);
        }
    }
}
