import { Routes } from '@angular/router';
import { Dashboard } from '../pages/dashboard/dashboard';
import { authGuard } from '../services/auth/auth-guard';


export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
];
