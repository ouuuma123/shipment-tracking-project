import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { WebsocketService } from '../services/websocket-service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgClass } from '@angular/common';

@Component({
  imports: [NgClass],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header implements OnInit {
  private destroyRef = inject(DestroyRef);
  private webSocketService = inject(WebsocketService);
  title = 'Shipmet Tracker';
  isConnected = signal(false);

  ngOnInit(): void {
    this.webSocketService
      .isConnected()
      .pipe(takeUntilDestroyed(this.destroyRef)) // permet de désabonner de l'observable lorsque le component est détruit
      .subscribe((connected) => this.isConnected.set(connected));
  }
}
