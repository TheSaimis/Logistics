import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_URL } from './api';
import { TokenResponse, UserDto } from './models';

const ACCESS_KEY = 'lg_access';
const REFRESH_KEY = 'lg_refresh';
const USER_KEY = 'lg_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  readonly user = signal<UserDto | null>(this.restoreUser());
  readonly isLoggedIn = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.roles.includes('ADMIN') ?? false);
  readonly canEdit = computed(() => {
    const roles = this.user()?.roles ?? [];
    return roles.includes('ADMIN') || roles.includes('MANAGER');
  });

  get accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  get refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/login`, { email, password })
      .pipe(tap((res) => this.storeSession(res)));
  }

  register(email: string, password: string, fullName: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/register`, { email, password, fullName })
      .pipe(tap((res) => this.storeSession(res)));
  }

  refresh(): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/refresh`, { refreshToken: this.refreshToken })
      .pipe(tap((res) => this.storeSession(res)));
  }

  /** Used by the OAuth2 callback page: store tokens, then load the profile. */
  acceptOAuthTokens(accessToken: string, refreshToken: string): Observable<UserDto> {
    localStorage.setItem(ACCESS_KEY, accessToken);
    localStorage.setItem(REFRESH_KEY, refreshToken);
    return this.http.get<UserDto>(`${API_URL}/auth/me`).pipe(
      tap((user) => {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        this.user.set(user);
      })
    );
  }

  logout(): void {
    const refreshToken = this.refreshToken;
    if (refreshToken) {
      this.http.post(`${API_URL}/auth/logout`, { refreshToken }).subscribe({ error: () => {} });
    }
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.user.set(null);
    this.router.navigate(['/login']);
  }

  private storeSession(res: TokenResponse): void {
    localStorage.setItem(ACCESS_KEY, res.accessToken);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this.user.set(res.user);
  }

  private restoreUser(): UserDto | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserDto;
    } catch {
      return null;
    }
  }
}
