import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ShipmentService } from '../services/shipment-service';
import { WebsocketService } from '../services/websocket-service';
import { ShipmentGrid } from './shipment-grid';

describe('ShipmentGrid', () => {
  let component: ShipmentGrid;
  let fixture: ComponentFixture<ShipmentGrid>;

  beforeEach(async () => {
    const shipmentService = {
      getAllShipments: () => of([]),
    };
    const websocketService = {
      connect: () => undefined,
      getStatusUpdates: () => of(null),
    };

    await TestBed.configureTestingModule({
      imports: [ShipmentGrid],
      providers: [
        { provide: ShipmentService, useValue: shipmentService },
        { provide: WebsocketService, useValue: websocketService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ShipmentGrid);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
