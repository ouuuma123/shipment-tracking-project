import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ShipmentStatus, STATUS_BADGE_CLASS, STATUS_LABELS, StatusUpdateMessage } from '../models/shipment.model';
import { WebsocketService } from '../services/websocket-service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, NgClass } from '@angular/common';

@Component({
  imports: [NgClass, DatePipe],
  selector: 'app-notification',
  styleUrl: './notification.css',
  templateUrl: './notification.html',
})
export class Notification implements OnInit{
  private destroyRef = inject(DestroyRef);
  private websocketService = inject(WebsocketService);
  STATUS_LABELS = STATUS_LABELS;
  STATUS_BADGE_CLASS_MAP = STATUS_BADGE_CLASS;
  notifications = signal<StatusUpdateMessage[]>([]);

  getStatusBadgeClass(status: ShipmentStatus): string {
    return this.STATUS_BADGE_CLASS_MAP[status];
  }

  ngOnInit(): void {
    this.handleUpdate();
  }

  private handleUpdate(): void {
    this.websocketService
      .getStatusUpdates()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((update) => {
        if (update) {
          this.notifications.update((notifications) => [update, ...notifications]);
        }
      });
  }

}
