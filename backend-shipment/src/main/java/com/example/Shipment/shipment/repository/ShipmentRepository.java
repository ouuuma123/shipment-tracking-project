package com.example.Shipment.shipment.repository;

import com.example.Shipment.shipment.entitie.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    public Optional<Shipment> findByTrackingNumber(String trackingNumber);

}
