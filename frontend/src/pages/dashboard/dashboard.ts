import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth/auth-service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly auth = inject(AuthService);
  protected readonly username = this.auth.getUsername();

  constructor() {
    const http = inject(HttpClient);
    const token = this.auth.getAccessToken();

    if (token) {
      http.get('/api/v1/auth/me', {
        headers: { Authorization: `Bearer ${token}` },
      }).subscribe();
    }
  }
}
