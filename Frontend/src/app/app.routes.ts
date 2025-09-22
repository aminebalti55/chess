// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'auth/login' },

  { 
    path: 'auth/login', 
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) 
  },

  { 
    path: 'auth/register', 
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) 
  },

  {
    path: 'lobby',
    canActivate: [AuthGuard],
    loadComponent: () => import('./features/lobby/lobby.component').then(m => m.LobbyComponent)
  },

  {
    path: 'game/:id',
    canActivate: [AuthGuard],
    loadComponent: () => import('./features/game/game.component').then(m => m.GameComponent)
  },

  { path: '**', redirectTo: 'auth/login' }
];