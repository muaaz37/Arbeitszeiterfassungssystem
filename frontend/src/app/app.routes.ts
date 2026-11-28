import { Routes } from '@angular/router';
import { Dashboard } from '../pages/dashboard/dashboard';
import { LoginPage } from '../pages/login-page/login-page';
import { authGuard } from '../services/auth/auth-guard';


export const routes: Routes = [
  { path: '', pathMatch: 'full', component: LoginPage },
  { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
];
