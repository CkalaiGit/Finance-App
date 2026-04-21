// src/app/user/user.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserContext {
  sub: string;
  email: string;
  username: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8081/api/users'; // adapte le port

  getMe(): Observable<UserContext> {
    return this.http.get<UserContext>(`${this.apiUrl}/me`);
  }
}