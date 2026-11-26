import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { authGuard } from './auth-guard';
import { AuthService } from './auth-service';

describe('authGuard', () => {
  const auth = {
    isAuthenticated: vi.fn(),
    login: vi.fn().mockResolvedValue(undefined),
  };

  beforeEach(() => {
    auth.login.mockClear();

    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: auth }],
    });
  });

  function runGuard() {
    return TestBed.runInInjectionContext(() =>
      authGuard(
        new ActivatedRouteSnapshot(),
        { url: '/dashboard' } as RouterStateSnapshot,
      ),
    );
  }

  it('erlaubt angemeldeten Benutzern den Zugriff', async () => {
    auth.isAuthenticated.mockReturnValue(true);

    expect(await runGuard()).toBe(true);
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('startet für anonyme Benutzer den Keycloak-Login', async () => {
    auth.isAuthenticated.mockReturnValue(false);

    expect(await runGuard()).toBe(false);
    expect(auth.login).toHaveBeenCalledOnce();
  });
});
