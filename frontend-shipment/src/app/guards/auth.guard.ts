import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth-service';
import { UserRole } from '../models/user.model';

export const authGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return (await authService.restoreSession()) || router.createUrlTree(['/login']);
};

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.currentUser()?.role === 'ADMIN' || router.createUrlTree(['/']);
};

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const allowedRoles = route.data['roles'] as UserRole[] | undefined;
  const currentRole = authService.currentUser()?.role;

  return (currentRole !== undefined && allowedRoles?.includes(currentRole)) || router.createUrlTree(['/']);
};
