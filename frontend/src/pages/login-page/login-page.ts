import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth/auth-service';

@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.html',
})
export class LoginPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly loginFehlgeschlagen = signal(false);

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      void this.router.navigateByUrl('/dashboard', { replaceUrl: true });
    }
  }

  protected async anmelden(): Promise<void> {
    this.loginFehlgeschlagen.set(false);

    try {
      await this.auth.login();
    } catch {
      this.loginFehlgeschlagen.set(true);
    }
  }
}
