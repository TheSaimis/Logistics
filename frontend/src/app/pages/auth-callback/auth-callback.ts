import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-auth-callback',
  imports: [RouterLink],
  template: `
    <div class="callback">
      @if (error()) {
        <div class="card">
          <h2>Sign-in failed</h2>
          <p class="muted">{{ error() }}</p>
          <a routerLink="/login" class="btn">Back to login</a>
        </div>
      } @else {
        <div class="card">
          <h2>Signing you in…</h2>
        </div>
      }
    </div>
  `,
  styles: `
    .callback {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .card { text-align: center; padding: 2rem 3rem; }
  `,
})
export class AuthCallbackPage implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);

  error = signal('');

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const token = params.get('token');
    const refresh = params.get('refresh');
    if (!token || !refresh) {
      this.error.set('Missing tokens in the callback URL.');
      return;
    }
    this.auth.acceptOAuthTokens(token, refresh).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => this.error.set('Could not load your profile.'),
    });
  }
}
