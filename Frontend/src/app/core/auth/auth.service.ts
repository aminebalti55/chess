// src/app/core/auth/auth.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, tap, catchError } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { RegisterRequest, LoginRequest, AuthResponse, CurrentUserDto } from '../models/dto';

const TOKEN_KEY = 'chess.jwt';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  
  private _currentUser = new BehaviorSubject<CurrentUserDto | null>(null);
  currentUser$ = this._currentUser.asObservable();

  constructor() {
    const token = this.getToken();
    if (token) {
      this.me().subscribe(); // lazy restore; ignore errors (token might be stale)
    }
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
  }

  private clearToken() {
    localStorage.removeItem(TOKEN_KEY);
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload).pipe(
      tap(res => {
        this.setToken(res.token);
        // Immediately set user data from response
        this._currentUser.next({
          id: res.userId,
          email: payload.email,
          displayName: res.displayName
        });
        // Also fetch full user data
        this.me().subscribe();
      })
    );
  }

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, payload).pipe(
      tap(res => {
        this.setToken(res.token);
        // Immediately set minimal user data
        this._currentUser.next({
          id: res.userId,
          email: payload.email,
          displayName: res.displayName
        });
        // Also fetch full user data
        this.me().subscribe();
      })
    );
  }

  me(): Observable<CurrentUserDto | null> {
    return this.http.get<CurrentUserDto>(`${environment.apiUrl}/users/me`).pipe(
      tap(user => this._currentUser.next(user)),
      catchError(() => {
        // token invalid/expired
        this._currentUser.next(null);
        this.clearToken();
        return of(null);
      })
    );
  }

  logout() {
    this.clearToken();
    this._currentUser.next(null);
    this.router.navigateByUrl('/auth/login');
  }
}