import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth-service';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-login-page',
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly authService = inject(AuthService);
  readonly submitting = signal(false);

  readonly loginForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  constructor() {
    void this.redirectIfAuthenticated();
  }

  async submit(): Promise<void> {
    if (this.loginForm.invalid || this.submitting()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const { email, password } = this.loginForm.getRawValue();
    const loggedIn = await this.authService.login(email, password);
    this.submitting.set(false);
    if (loggedIn) {
      await this.router.navigateByUrl('/');
    }
  }

  private async redirectIfAuthenticated(): Promise<void> {
    if (await this.authService.restoreSession()) {
      await this.router.navigateByUrl('/');
    }
  }
}
