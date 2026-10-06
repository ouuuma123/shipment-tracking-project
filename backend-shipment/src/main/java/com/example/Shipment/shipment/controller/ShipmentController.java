package com.example.Shipment.shipment.controller;

import com.example.Shipment.shipment.DTO.ShipmentDTO;
import com.example.Shipment.shipment.entitie.Shipment;
import com.example.Shipment.shipment.entitie.ShipmentStatus;
import com.example.Shipment.shipment.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@AllArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ShipmentDTO.ShipmentResponse> createShipment(@Valid @RequestBody ShipmentDTO.CreateShipmentRequest request) {
        ShipmentDTO.ShipmentResponse shipment = shipmentService.createShipment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
    }

    @GetMapping
    public ResponseEntity<List<ShipmentDTO.ShipmentResponse>> getAllShipments() {
        List<ShipmentDTO.ShipmentResponse> shipments = shipmentService.getAllShipments();

        return ResponseEntity.status(HttpStatus.OK).body(shipments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentDTO.ShipmentResponse> getShipmentById(@PathVariable Long id) {
        ShipmentDTO.ShipmentResponse shipment = shipmentService.getShipmentById(id);

        return ResponseEntity.status(HttpStatus.OK).body(shipment);
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ShipmentDTO.ShipmentResponse> getShipmentByTrackingNumber(@PathVariable String trackingNumber) {
        ShipmentDTO.ShipmentResponse shipment = shipmentService.getShipmentByTrackingNumber(trackingNumber);

        return ResponseEntity.status(HttpStatus.OK).body(shipment);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ShipmentDTO.ShipmentResponse> updateShipmentStatus(@PathVariable Long id,@Valid @RequestBody ShipmentDTO.UpdateStatusRequest request) {
        ShipmentDTO.ShipmentResponse shipment = shipmentService.updateShipmentStatus(id, request);

        return ResponseEntity.status(HttpStatus.OK).body(shipment);
    }

}
