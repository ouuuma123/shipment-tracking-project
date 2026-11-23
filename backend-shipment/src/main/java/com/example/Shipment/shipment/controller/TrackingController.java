package com.example.Shipment.shipment.controller;

import com.example.Shipment.shipment.DTO.ShipmentDTO;
import com.example.Shipment.shipment.service.ShipmentService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/track")
@AllArgsConstructor
public class TrackingController {

    private final ShipmentService shipmentService;

    @GetMapping("/{trackingNumber}")
    public ShipmentDTO.ShipmentResponse track(@PathVariable String trackingNumber) {
        return shipmentService.getShipmentByTrackingNumber(trackingNumber);
    }
}
