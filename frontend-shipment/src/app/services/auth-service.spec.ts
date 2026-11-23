import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from './auth-service';

const user = {
  id: 1,
  fullName: 'Test Client',
  email: 'client@example.test',
  role: 'CLIENT' as const,
  createdAt: '2026-01-01T00:00:00',
};

describe('AuthService', () => {
  let authService: AuthService;
  let httpClient: { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    httpClient = {
      get: vi.fn().mockReturnValue(of({ token: 'csrf-token' })),
      post: vi.fn().mockReturnValue(of({
        accessToken: 'access-token',
        tokenType: 'Bearer',
        expiresIn: 900,
        user,
      })),
    };
    TestBed.configureTestingModule({
      providers: [AuthService, { provide: HttpClient, useValue: httpClient }],
    });
    authService = TestBed.inject(AuthService);
  });

  it('stores the access token in memory after login', async () => {
    expect(await authService.login('client@example.test', 'Client-password-91')).toBe(true);

    expect(httpClient.get).toHaveBeenCalledWith('http://localhost:8080/api/auth/csrf');
    expect(httpClient.post).toHaveBeenCalledWith(
      'http://localhost:8080/api/auth/login',
      { email: 'client@example.test', password: 'Client-password-91' },
    );
    expect(authService.accessToken()).toBe('access-token');
    expect(authService.currentUser()?.role).toBe('CLIENT');
  });

  it('restores a session through the refresh endpoint', async () => {
    expect(await authService.refreshAccessToken()).toBe('access-token');

    expect(httpClient.post).toHaveBeenCalledWith('http://localhost:8080/api/auth/refresh', {});
    expect(authService.accessToken()).toBe('access-token');
    expect(authService.currentUser()).toEqual(user);
  });
});
