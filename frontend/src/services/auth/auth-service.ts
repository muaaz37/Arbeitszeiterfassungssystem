import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { authConfig } from './auth-config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = new Keycloak(authConfig);

  init(): Promise<boolean> {
    return this.keycloak.init({ pkceMethod: 'S256' });
  }

  isAuthenticated(): boolean {
    return !!this.keycloak.token && !this.keycloak.isTokenExpired();
  }

  login(): Promise<void> {
    return this.keycloak.login({
      redirectUri: `${window.location.origin}/`,
    });
  }

  getUsername(): string {
    const claims = this.keycloak.tokenParsed;
    const username = claims?.['preferred_username'];

    if (typeof username === 'string' && username.length > 0) {
      return username;
    }

    return claims?.sub ?? '';
  }

  getAccessToken(): string | undefined {
    return this.isAuthenticated() ? this.keycloak.token : undefined;
  }
}

