import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CreateUserRequest, UserProfile } from '../models/user.model';

@Service()
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/users`;

  getUsers(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(this.apiUrl);
  }

  createUser(request: CreateUserRequest): Observable<UserProfile> {
    return this.http.post<UserProfile>(this.apiUrl, request);
  }
}
