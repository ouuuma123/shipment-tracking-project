package com.example.Shipment.shipment.service;

import com.example.Shipment.shipment.DTO.ShipmentDTO;
import com.example.Shipment.shipment.entitie.Shipment;
import com.example.Shipment.shipment.entitie.ShipmentStatus;
import com.example.Shipment.shipment.repository.ShipmentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ShipmentDTO.ShipmentResponse createShipment(ShipmentDTO.CreateShipmentRequest request) {
        String trackNumber = generateTrackingNumber();

        Shipment shipment = Shipment.builder()
                .trackingNumber(trackNumber)
                .origin(request.origin)
                .destination(request.destination)
                .estimatedDelivery(request.estimatedDelivery)
                .build();
        shipmentRepository.save(shipment);
        notifyShipmentStatus(shipment, getStatusMessage(shipment.getStatus()));

        return mapToResponse(shipment);
    }

    public List<ShipmentDTO.ShipmentResponse> getAllShipments() {
        List<Shipment> shipments = shipmentRepository.findAll();

        return shipments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ShipmentDTO.ShipmentResponse getShipmentById(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found with id: " + id));

        return mapToResponse(shipment);
    }

    public ShipmentDTO.ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Shipment not found with tracking number : " + trackingNumber));

        return mapToResponse(shipment);
    }

    public ShipmentDTO.ShipmentResponse updateShipmentStatus(Long id, ShipmentDTO.UpdateStatusRequest request) {

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found with id : " + id));

        shipment.setStatus(request.getStatus());
        if(request.getCurrentLocation() != null) {
            shipment.setCurrentLocation(request.getCurrentLocation());
        }
        shipment = shipmentRepository.save(shipment);

        notifyShipmentStatus(shipment, getStatusMessage(shipment.getStatus()));

        return mapToResponse(shipment);
    }

    private String generateTrackingNumber() {
        return "TR-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
    }

    private ShipmentDTO.ShipmentResponse mapToResponse(Shipment shipment) {
        return ShipmentDTO.ShipmentResponse.builder()
                .id(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .origin(shipment.getOrigin())
                .destination(shipment.getDestination())
                .status(shipment.getStatus())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .currentLocation(shipment.getCurrentLocation())
                .estimatedDelivery(shipment.getEstimatedDelivery())
                .build();
    }

    public void notifyShipmentStatus(Shipment shipment, String message) {
        ShipmentDTO.StatusUpdateMessage statusUpdateMessage = ShipmentDTO.StatusUpdateMessage.builder()
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus())
                .currentLocation(shipment.getCurrentLocation())
                .timestamp(shipment.getUpdatedAt())
                .message(message)
                .build();;

        messagingTemplate.convertAndSend("/topic/shipments" , statusUpdateMessage);
        messagingTemplate.convertAndSend("/topic/shipments" + shipment.getId(), statusUpdateMessage);

        log.info("Sent shipment status update: {}", statusUpdateMessage);
    }

    private String getStatusMessage(ShipmentStatus status) {
        return switch (status) {
            case ORDER_PLACED -> "Order has been placed";
            case PROCESSING -> "Order is being processed";
            case PICKED_UP -> "Package has been picked up";
            case IN_TRANSIT -> "Package is in transit";
            case OUT_FOR_DELIVERY -> "Package is out for delivery";
            case DELIVERED -> "Package has been delivered";
            case EXCEPTION -> "Delivery exception occurred";
        };
    }
}
