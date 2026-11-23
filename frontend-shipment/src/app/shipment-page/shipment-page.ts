import { Component } from '@angular/core';
import { CreateShipment } from '../create-shipment/create-shipment';
import { Notification } from '../notification/notification';
import { ShipmentGrid } from '../shipment-grid/shipment-grid';
import { UpdateShipment } from '../update-shipment/update-shipment';

@Component({
  imports: [CreateShipment, UpdateShipment, Notification, ShipmentGrid],
  selector: 'app-shipment-page',
  templateUrl: './shipment-page.html',
})
export class ShipmentPage {}
