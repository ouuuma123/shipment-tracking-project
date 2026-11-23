package com.example.Shipment.shipment.service;

import com.example.Shipment.shipment.DTO.ShipmentDTO;
import com.example.Shipment.shipment.entitie.Shipment;
import com.example.Shipment.shipment.entitie.ShipmentStatus;
import com.example.Shipment.shipment.repository.ShipmentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ShipmentDTO.ShipmentResponse createShipment(ShipmentDTO.CreateShipmentRequest request) {
        for (int attempt = 0; attempt < 3; attempt++) {
            Shipment shipment = Shipment.builder()
                    .trackingNumber(generateTrackingNumber())
                    .origin(request.origin)
                    .destination(request.destination)
                    .estimatedDelivery(request.estimatedDelivery)
                    .build();
            try {
                shipmentRepository.saveAndFlush(shipment);
                notifyShipmentStatus(shipment, getStatusMessage(shipment.getStatus()));
                return mapToResponse(shipment);
            } catch (DataIntegrityViolationException exception) {
                if (attempt == 2) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("Unable to allocate a tracking number.");
    }

    public List<ShipmentDTO.ShipmentResponse> getAllShipments() {
        List<Shipment> shipments = shipmentRepository.findAll();

        return shipments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ShipmentDTO.ShipmentResponse getShipmentById(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(ShipmentNotFoundException::new);

        return mapToResponse(shipment);
    }

    public ShipmentDTO.ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(ShipmentNotFoundException::new);

        return mapToResponse(shipment);
    }

    public ShipmentDTO.ShipmentResponse updateShipmentStatus(Long id, ShipmentDTO.UpdateStatusRequest request) {

        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(ShipmentNotFoundException::new);

        ShipmentStatusPolicy.requireTransition(shipment.getStatus(), request.getStatus());
        shipment.setStatus(request.getStatus());
        if(request.getCurrentLocation() != null) {
            shipment.setCurrentLocation(request.getCurrentLocation());
        }
        shipment = shipmentRepository.save(shipment);

        notifyShipmentStatus(shipment, getStatusMessage(shipment.getStatus()));

        return mapToResponse(shipment);
    }

    private String generateTrackingNumber() {
        String prefix = "SHP-" + Year.now().getValue() + "-";
        String latest = shipmentRepository.findMaxTrackingNumber(prefix);
        int next = 1;
        if (latest != null && latest.startsWith(prefix)) {
            next = Integer.parseInt(latest.substring(prefix.length())) + 1;
        }
        if (next > 999_999) {
            throw new IllegalStateException("The tracking number sequence is exhausted for this year.");
        }
        return prefix + String.format(Locale.ROOT, "%06d", next);
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
                .build();

        messagingTemplate.convertAndSend("/topic/shipments", statusUpdateMessage);
        messagingTemplate.convertAndSend("/topic/shipments/" + shipment.getId(), statusUpdateMessage);

        log.info("Sent shipment status update: {}", statusUpdateMessage);
    }

    private String getStatusMessage(ShipmentStatus status) {
        return switch (status) {
            case CREATED -> "Shipment has been created";
            case PICKED_UP -> "Package has been picked up";
            case IN_TRANSIT -> "Package is in transit";
            case OUT_FOR_DELIVERY -> "Package is out for delivery";
            case DELIVERED -> "Package has been delivered";
            case FAILED_DELIVERY -> "Delivery attempt failed";
            case RETURNED -> "Package has been returned";
            case CANCELLED -> "Shipment has been cancelled";
        };
    }
}
