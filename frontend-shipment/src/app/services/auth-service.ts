import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserProfile } from '../models/user.model';

interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  user: UserProfile;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
}

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/auth`;
  private restored = false;
  private restorePromise?: Promise<boolean>;
  private refreshPromise?: Promise<string | null>;
  private accessTokenExpiresAt = 0;

  readonly currentUser = signal<UserProfile | null>(null);
  readonly accessToken = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  restoreSession(): Promise<boolean> {
    if (this.restored) {
      return Promise.resolve(this.currentUser() !== null);
    }
    if (this.restorePromise) {
      return this.restorePromise;
    }

    this.restorePromise = this.refreshAccessToken()
      .then(async (token) => {
        if (!token) {
          this.restored = true;
          return false;
        }
        try {
          const user = await firstValueFrom(this.http.get<UserProfile>(`${this.apiUrl}/me`));
          this.currentUser.set(user);
          this.restored = true;
          return true;
        } catch (error: unknown) {
          this.clearSession();
          this.restored = true;
          this.error.set(this.errorMessage(error));
          return false;
        }
      })
      .finally(() => {
        this.restorePromise = undefined;
      });

    return this.restorePromise;
  }

  async login(email: string, password: string): Promise<boolean> {
    this.error.set(null);
    try {
      await this.fetchCsrfToken();
      const response = await firstValueFrom(
        this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }),
      );
      this.setSession(response);
      this.restored = true;
      return true;
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
      return false;
    }
  }

  async register(request: RegisterRequest): Promise<boolean> {
    this.error.set(null);
    try {
      await this.fetchCsrfToken();
      const response = await firstValueFrom(
        this.http.post<AuthResponse>(`${this.apiUrl}/register`, request),
      );
      this.setSession(response);
      this.restored = true;
      return true;
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
      return false;
    }
  }

  async logout(): Promise<boolean> {
    this.error.set(null);
    try {
      await this.fetchCsrfToken();
      await firstValueFrom(this.http.post<void>(`${this.apiUrl}/logout`, null));
      this.clearSession();
      this.restored = true;
      return true;
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
      return false;
    }
  }

  async changePassword(currentPassword: string, newPassword: string): Promise<boolean> {
    this.error.set(null);
    try {
      await firstValueFrom(
        this.http.post(`${this.apiUrl}/password/change`, { currentPassword, newPassword }),
      );
      this.clearSession();
      this.restored = true;
      return true;
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
      return false;
    }
  }

  async requestPasswordReset(email: string): Promise<boolean> {
    this.error.set(null);
    try {
      await this.fetchCsrfToken();
      await firstValueFrom(this.http.post(`${this.apiUrl}/password/forgot`, { email }));
      return true;
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
      return false;
    }
  }

  async resetPassword(token: string, newPassword: string): Promise<boolean> {
    this.error.set(null);
    try {
      await this.fetchCsrfToken();
      await firstValueFrom(this.http.post(`${this.apiUrl}/password/reset`, { token, newPassword }));
      return true;
    } catch (error: unknown) {
      this.error.set(this.errorMessage(error));
      return false;
    }
  }

  refreshAccessToken(): Promise<string | null> {
    if (this.refreshPromise) {
      return this.refreshPromise;
    }
    this.refreshPromise = this.refreshSession()
      .catch((error: unknown) => {
        if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
          this.error.set(this.errorMessage(error));
        }
        this.clearSession();
        return null;
      })
      .finally(() => {
        this.refreshPromise = undefined;
      });
    return this.refreshPromise;
  }

  getValidAccessToken(): Promise<string | null> {
    if (this.accessToken() && this.accessTokenExpiresAt > Date.now() + 30_000) {
      return Promise.resolve(this.accessToken());
    }
    return this.refreshAccessToken();
  }

  private async refreshSession(): Promise<string> {
    await this.fetchCsrfToken();
    const response = await firstValueFrom(
      this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, {}),
    );
    this.setSession(response);
    return response.accessToken;
  }

  private async fetchCsrfToken(): Promise<void> {
    await firstValueFrom(this.http.get(`${this.apiUrl}/csrf`));
  }

  private setSession(response: AuthResponse): void {
    this.accessToken.set(response.accessToken);
    this.accessTokenExpiresAt = Date.now() + response.expiresIn * 1000;
    this.currentUser.set(response.user);
    this.error.set(null);
  }

  private clearSession(): void {
    this.accessToken.set(null);
    this.accessTokenExpiresAt = 0;
    this.currentUser.set(null);
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'Cette adresse e-mail est déjà utilisée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 401) {
      return 'Adresse e-mail ou mot de passe incorrect, ou session expirée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Impossible de joindre le serveur. Vérifiez que le backend est démarré.';
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'Vous ne disposez pas des droits nécessaires.';
    }
    if (error instanceof HttpErrorResponse && error.status === 400) {
      return error.error?.message ?? 'Les informations fournies ne sont pas valides.';
    }
    return 'Une erreur est survenue. Veuillez réessayer.';
  }
}
