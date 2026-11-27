import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiAccessError } from '../../services/auth/api-access-error';
import { AuthMeService } from '../../services/auth/auth-me-service';
import { AuthService } from '../../services/auth/auth-service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  private readonly authMe = inject(AuthMeService);

  protected readonly username = signal('');
  protected readonly problem = signal<{ message: string; showLogin: boolean } | null>(null);

  constructor() {
    this.authMe.getMe().pipe(takeUntilDestroyed()).subscribe({
      next: (user) => this.username.set(user.username),
      error: (error: unknown) => {
        const apiError = error instanceof ApiAccessError ? error : null;

        this.problem.set({
          message: apiError?.message ?? 'Die Benutzeridentität konnte nicht geladen werden.',
          showLogin: apiError?.kind === 'session',
        });
      },
    });
  }

  protected async login(): Promise<void> {
    try {
      await this.auth.login(true);
    } catch {
      this.problem.set({
        message: 'Die Anmeldung konnte nicht gestartet werden. Bitte versuche es erneut.',
        showLogin: true,
      });
    }
  }
}
