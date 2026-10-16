// src/app/user/user.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UserContext {
   sub: string;
   email: string;
   username: string;
   roles: string[];
}

@Injectable({ providedIn: 'root' })
export class UserService {
   private readonly http = inject(HttpClient);
   private readonly apiUrl = `${environment.apiUrl.replace('/v1', '')}/users`;

   getMe(): Observable<UserContext> {
      return this.http.get<UserContext>(`${this.apiUrl}/me`);
   }
}
