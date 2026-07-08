import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/guards';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.LoginPage),
  },
  {
    path: 'auth/callback',
    loadComponent: () => import('./pages/auth-callback/auth-callback').then((m) => m.AuthCallbackPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/layout/layout').then((m) => m.LayoutPage),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.DashboardPage),
      },
      {
        path: 'products',
        loadComponent: () => import('./pages/products/products').then((m) => m.ProductsPage),
      },
      {
        path: 'movements',
        loadComponent: () => import('./pages/movements/movements').then((m) => m.MovementsPage),
      },
      {
        path: 'warehouses',
        loadComponent: () => import('./pages/warehouses/warehouses').then((m) => m.WarehousesPage),
      },
      {
        path: 'partners',
        loadComponent: () => import('./pages/partners/partners').then((m) => m.PartnersPage),
      },
      {
        path: 'admin/users',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/admin-users/admin-users').then((m) => m.AdminUsersPage),
      },
      {
        path: 'admin/analytics',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./pages/admin-analytics/admin-analytics').then((m) => m.AdminAnalyticsPage),
      },
      {
        path: 'admin/audit',
        canActivate: [adminGuard],
        loadComponent: () => import('./pages/admin-audit/admin-audit').then((m) => m.AdminAuditPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
