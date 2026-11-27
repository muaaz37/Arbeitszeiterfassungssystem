import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig).catch(() => {
  const status = document.getElementById('auth-startup');

  if (status) {
    status.textContent = 'Die Anmeldung konnte nicht gestartet werden. Bitte lade die Seite neu.';
  }
});
