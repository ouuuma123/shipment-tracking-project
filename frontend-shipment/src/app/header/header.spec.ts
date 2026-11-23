import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { UserProfile } from '../models/user.model';
import { AuthService } from '../services/auth-service';
import { WebsocketService } from '../services/websocket-service';
import { Header } from './header';

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;

  beforeEach(async () => {
    const authService = {
      currentUser: signal<UserProfile | null>(null),
      error: signal<string | null>(null),
      logout: async () => true,
    };
    const websocketService = {
      disconnect: () => undefined,
      isConnected: () => of(false),
    };

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: WebsocketService, useValue: websocketService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
