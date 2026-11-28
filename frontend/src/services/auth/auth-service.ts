import { inject, Injectable, InjectionToken } from '@angular/core';
import Keycloak from 'keycloak-js';
import { ApiAccessError } from './api-access-error';
import { authConfig } from './auth-config';

export const KEYCLOAK_CLIENT = new InjectionToken<Keycloak>('Keycloak client', {
  providedIn: 'root',
  factory: () => new Keycloak(authConfig),
});

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = inject(KEYCLOAK_CLIENT);
  private sessionInvalidated = false;

  init(): Promise<boolean> {
    return this.keycloak.init({ pkceMethod: 'S256' });
  }

  isAuthenticated(): boolean {
    return !this.sessionInvalidated && this.keycloak.authenticated === true;
  }

  login(forceLogin = false): Promise<void> {
    return this.keycloak.login({
      redirectUri: `${window.location.origin}/`,
      prompt: forceLogin ? 'login' : undefined,
    });
  }

  async logout(): Promise<void> {
    this.sessionInvalidated = true;

    try {
      await this.keycloak.logout({
        redirectUri: `${window.location.origin}/`,
      });
    } catch (error) {
      this.sessionInvalidated = false;
      throw error;
    }

    this.invalidateSession();
  }

  invalidateSession(): void {
    this.sessionInvalidated = true;
    void this.keycloak.clearToken().catch(() => undefined);
  }

  async getAccessToken(): Promise<string> {
    if (!this.isAuthenticated()) {
      throw new ApiAccessError('session');
    }

    try {
      await this.keycloak.updateToken(30);
      const token = this.keycloak.token;

      if (!this.sessionInvalidated && token && !this.keycloak.isTokenExpired()) {
        return token;
      }
    } catch {
      // A failed refresh is handled below.
    }

    const shouldLogin = !this.sessionInvalidated;
    this.invalidateSession();

    if (shouldLogin) {
      void this.login(true).catch(() => undefined);
    }

    throw new ApiAccessError('session');
  }

  getAccountUrl(): string {
    return this.keycloak.createAccountUrl({
      redirectUri: `${window.location.origin}/`,
    });
  }
}
