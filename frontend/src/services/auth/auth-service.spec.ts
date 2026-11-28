import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authGuard } from './auth-guard';
import { AuthService, KEYCLOAK_CLIENT } from './auth-service';

describe('AuthService.logout', () => {
  let auth: AuthService;

  const keycloak = {
    authenticated: true,
    logout: vi.fn(),
    clearToken: vi.fn(),
    login: vi.fn(),
  };

  beforeEach(() => {
    keycloak.authenticated = true;
    keycloak.logout.mockReset().mockResolvedValue(undefined);
    keycloak.login.mockReset().mockResolvedValue(undefined);
    keycloak.clearToken.mockReset().mockImplementation(async () => {
      keycloak.authenticated = false;
    });

    TestBed.configureTestingModule({
      providers: [{ provide: KEYCLOAK_CLIENT, useValue: keycloak }],
    });

    auth = TestBed.inject(AuthService);
  });

  it('startet den Keycloak-Logout und sperrt den lokalen Token-Zugriff', async () => {
    await auth.logout();

    expect(keycloak.logout).toHaveBeenCalledWith({
      redirectUri: `${window.location.origin}/`,
    });
    expect(keycloak.clearToken).toHaveBeenCalledOnce();
    expect(auth.isAuthenticated()).toBe(false);
    await expect(auth.getAccessToken()).rejects.toMatchObject({ kind: 'session' });
  });

  it('gibt einen fehlgeschlagenen Logout an die Oberfläche weiter', async () => {
    const fehler = new Error('Logout konnte nicht gestartet werden');
    keycloak.logout.mockRejectedValue(fehler);

    await expect(auth.logout()).rejects.toBe(fehler);

    expect(keycloak.clearToken).not.toHaveBeenCalled();
    expect(auth.isAuthenticated()).toBe(true);
  });

  it('fordert beim Aufruf einer geschützten Route nach dem Logout eine Anmeldung an', async () => {
    await auth.logout();

    const zugriff = await TestBed.runInInjectionContext(() =>
      authGuard(
        new ActivatedRouteSnapshot(),
        { url: '/dashboard' } as RouterStateSnapshot,
      ),
    );

    expect(zugriff).toBe(false);
    expect(keycloak.login).toHaveBeenCalledOnce();
  });
});
