import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthMeService } from '../../services/auth/auth-me-service';
import { AuthService } from '../../services/auth/auth-service';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  const accountUrl =
    'http://localhost:8081/realms/arbeitszeiterfassung/account/';

  function erstelleDashboard(angemeldet: boolean) {
    const logout = vi.fn().mockResolvedValue(undefined);

    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => angemeldet,
            getAccountUrl: () => accountUrl,
            logout,
          },
        },
        {
          provide: AuthMeService,
          useValue: {
            getMe: () => of({ subject: 'test-id', username: 'test-user' }),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    const host: HTMLElement = fixture.nativeElement;
    return { fixture, host, logout };
  }

  it('verlinkt für angemeldete Benutzer die Keycloak-Kontoverwaltung', () => {
    const { host } = erstelleDashboard(true);
    const link = host.querySelector<HTMLAnchorElement>('a');

    expect(link?.textContent?.trim()).toBe('Passwort ändern');
    expect(link?.href).toBe(accountUrl);
    expect(link?.target).toBe('_blank');
  });

  it('blendet den Link zur Passwortänderung ohne Anmeldung aus', () => {
    const { host } = erstelleDashboard(false);

    expect(host.textContent).not.toContain('Passwort ändern');
  });

  it('ruft beim Klick auf Abmelden den AuthService auf', async () => {
    const { fixture, host, logout } = erstelleDashboard(true);
    const button = Array.from(host.querySelectorAll('button'))
      .find((element) => element.textContent?.trim() === 'Abmelden');

    expect(button).toBeDefined();
    button?.click();

    await fixture.whenStable();
    expect(logout).toHaveBeenCalledOnce();
  });
});
