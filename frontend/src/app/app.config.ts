import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { apiAuthInterceptor } from '../services/auth/api-auth-interceptor';
import { AuthService } from '../services/auth/auth-service';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideAppInitializer(() => inject(AuthService).init()),
    provideHttpClient(withInterceptors([apiAuthInterceptor])),
    provideRouter(routes),
  ],
};
