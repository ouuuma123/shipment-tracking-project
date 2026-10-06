import { Component, signal } from '@angular/core';
import { ShipmentGrid } from './shipment-grid/shipment-grid';
import { CreateShipment } from './create-shipment/create-shipment';
import { UpdateShipment } from './update-shipment/update-shipment';
import { Header } from './header/header';
import { Notification } from './notification/notification';

@Component({
  imports: [ShipmentGrid, CreateShipment, UpdateShipment, Header, Notification],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('shipment-frontEnd');
}
