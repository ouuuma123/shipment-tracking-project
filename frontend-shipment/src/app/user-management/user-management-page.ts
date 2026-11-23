import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserProfile, UserRole } from '../models/user.model';
import { UserService } from '../services/user-service';

@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-user-management-page',
  templateUrl: './user-management-page.html',
})
export class UserManagementPage implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly userService = inject(UserService);

  readonly users = signal<UserProfile[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly userForm = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]],
    role: ['OPERATOR' as UserRole, Validators.required],
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  createUser(): void {
    if (this.userForm.invalid || this.submitting()) {
      this.userForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.message.set(null);
    this.error.set(null);
    this.userService.createUser(this.userForm.getRawValue()).subscribe({
      next: (user) => {
        this.users.update((users) => [user, ...users]);
        this.userForm.reset({ fullName: '', email: '', password: '', role: 'OPERATOR' });
        this.message.set(`Le compte de ${user.fullName} a été créé.`);
        this.submitting.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error));
        this.submitting.set(false);
      },
    });
  }

  private loadUsers(): void {
    this.userService.getUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return 'Cette adresse e-mail est déjà utilisée.';
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'Impossible de joindre le serveur. Vérifiez que le backend est démarré.';
    }
    return 'Impossible de terminer cette action. Vérifiez vos droits et réessayez.';
  }
}
