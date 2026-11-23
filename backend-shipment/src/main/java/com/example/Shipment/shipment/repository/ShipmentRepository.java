package com.example.Shipment.shipment.repository;

import com.example.Shipment.shipment.entitie.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    @Query("select max(s.trackingNumber) from shipment s where s.trackingNumber like concat(:prefix, '%')")
    String findMaxTrackingNumber(@Param("prefix") String prefix);

}
