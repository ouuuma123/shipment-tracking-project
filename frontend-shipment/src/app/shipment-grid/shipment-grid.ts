import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ShipmentService } from '../services/shipment-service';
import { Shipment, ShipmentStatus, STATUS_BADGE_CLASS, STATUS_LABELS, StatusUpdateMessage } from '../models/shipment.model';
import { DatePipe, NgClass } from '@angular/common';
import { WebsocketService } from '../services/websocket-service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  imports: [DatePipe, NgClass],
  selector: 'app-shipment-grid',
  styleUrl: './shipment-grid.css',
  templateUrl: './shipment-grid.html',
})
export class ShipmentGrid implements OnInit{
  private destroyRef = inject(DestroyRef);
  private shipmentService = inject(ShipmentService);
  shipments = signal<Shipment[]>([]); // ici on déclare un signal de type SHipment[], càd un tableau de Shipment et on l'initialise avec un tableau vide
  private websocketService = inject(WebsocketService);

  STATUS_LABELS = STATUS_LABELS;
  STATUS_BADGE_CLASS_MAP = STATUS_BADGE_CLASS;

  ngOnInit(): void {
    console.log("Running !!");
    this.loadShipment();
    this.connectWebSocket();
  }

  private connectWebSocket(): void {
    this.websocketService.connect();
    this.handleUpdate();
  }

  private handleUpdate(): void {
    this.websocketService.getStatusUpdates()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((update) => {
        if(update) {
          this.handleStatusUpdate(update);
        }
      });
  }

  private handleStatusUpdate(update: StatusUpdateMessage): void {
    if (!this.shipments().some(shipment => shipment.id === update.shipmentId)) {
      this.loadShipment();
      return;
    }

    this.shipments.update(shipments => {
      const updatedShipment = shipments.find(shipment => shipment.id === update.shipmentId);
      if(!updatedShipment) {
        return shipments;
      } 
      const updatedShipments = shipments.map(shipment => {
        if(shipment.id !== updatedShipment.id) return shipment;

        return {
          ...shipment,
          status: update.status,
          currentLocation: update.currentLocation,
          updatedAt: update.timestamp,
        };
      });

      return updatedShipments;
    });
  }

  loadShipment(): void {
    this.shipmentService.getAllShipments()
      .subscribe((shipments) => {
        this.shipments.set(shipments);
      });
  }

  getStatusBadgeClass(status: ShipmentStatus): string {
    return this.STATUS_BADGE_CLASS_MAP[status];
  }

}
