import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { apiBaseUrl } from './api-config';

export interface AuthIdentity {
  subject: string;
  username: string;
}

@Injectable({ providedIn: 'root' })
export class AuthMeService {
  private readonly http = inject(HttpClient);

  getMe() {
    return this.http.get<AuthIdentity>(`${apiBaseUrl}/v1/auth/me`);
  }
}
