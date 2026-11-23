export type UserRole = 'ADMIN' | 'MANAGER' | 'DISPATCHER' | 'DRIVER' | 'CLIENT' | 'OPERATOR';

export interface UserProfile {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  createdAt: string;
}

export interface CreateUserRequest {
  fullName: string;
  email: string;
  password: string;
  role: UserRole;
}
