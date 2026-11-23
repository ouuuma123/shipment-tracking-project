import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './guards/auth.guard';
import { AppLayout } from './layout/app-layout';
import { LoginPage } from './login/login-page';
import { ShipmentPage } from './shipment-page/shipment-page';
import { UserManagementPage } from './user-management/user-management-page';
import { RegisterPage } from './register/register-page';
import { ResetPasswordPage } from './reset-password/reset-password-page';

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'register', component: RegisterPage },
  { path: 'reset-password', component: ResetPasswordPage },
  {
    path: '',
    component: AppLayout,
    canActivate: [authGuard],
    children: [
      { path: '', component: ShipmentPage },
      {
        path: 'admin/users',
        component: UserManagementPage,
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
