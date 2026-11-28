import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthMeService } from '../../services/auth/auth-me-service';
import { AuthService } from '../../services/auth/auth-service';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  it('ruft beim Klick auf Abmelden den AuthService auf', async () => {
    const logout = vi.fn().mockResolvedValue(undefined);

    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        {
          provide: AuthService,
          useValue: { isAuthenticated: () => true, logout },
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
    const button = Array.from(host.querySelectorAll('button'))
      .find((element) => element.textContent?.trim() === 'Abmelden');

    expect(button).toBeDefined();
    button?.click();

    await fixture.whenStable();
    expect(logout).toHaveBeenCalledOnce();
  });
});
