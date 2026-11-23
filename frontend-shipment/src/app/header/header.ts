import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { WebsocketService } from '../services/websocket-service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgClass } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth-service';
import { UserRole } from '../models/user.model';

@Component({
  imports: [NgClass, RouterLink],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header implements OnInit {
  private destroyRef = inject(DestroyRef);
  private webSocketService = inject(WebsocketService);
  private router = inject(Router);
  readonly authService = inject(AuthService);
  readonly title = 'Shipment Tracker';
  readonly isConnected = signal(false);
  readonly logoutError = signal<string | null>(null);

  roleLabel(): string {
    const labels: Record<UserRole, string> = {
      ADMIN: 'Administrateur',
      MANAGER: 'Manager',
      DISPATCHER: 'Dispatcher',
      DRIVER: 'Livreur',
      CLIENT: 'Client',
      OPERATOR: 'Opérateur',
    };
    const role = this.authService.currentUser()?.role;
    return role ? labels[role] : '';
  }

  ngOnInit(): void {
    this.webSocketService
      .isConnected()
      .pipe(takeUntilDestroyed(this.destroyRef)) // permet de désabonner de l'observable lorsque le component est détruit
      .subscribe((connected) => this.isConnected.set(connected));
  }

  async logout(): Promise<void> {
    this.logoutError.set(null);
    if (await this.authService.logout()) {
      this.webSocketService.disconnect();
      await this.router.navigateByUrl('/login');
    } else {
      this.logoutError.set(this.authService.error());
    }
  }
}
