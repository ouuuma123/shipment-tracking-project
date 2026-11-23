import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth-service';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  selector: 'app-reset-password-page',
  templateUrl: './reset-password-page.html',
})
export class ResetPasswordPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  readonly authService = inject(AuthService);
  readonly token = this.route.snapshot.queryParamMap.get('token');
  readonly submitting = signal(false);
  readonly completed = signal(false);

  readonly emailForm = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });
  readonly passwordForm = this.formBuilder.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]],
  });

  async requestReset(): Promise<void> {
    if (this.emailForm.invalid || this.submitting()) {
      this.emailForm.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.completed.set(await this.authService.requestPasswordReset(this.emailForm.getRawValue().email));
    this.submitting.set(false);
  }

  async resetPassword(): Promise<void> {
    const token = this.token;
    if (!token || this.passwordForm.invalid || this.submitting()) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.completed.set(await this.authService.resetPassword(token, this.passwordForm.getRawValue().password));
    this.submitting.set(false);
  }
}
