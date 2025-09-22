// src/app/app.component.ts
import { Component, inject, OnDestroy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/navbar/navbar.component';
import { AuthService } from './core/auth/auth.service';
import { StompService } from './core/ws/stomp.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar></app-navbar>
    <router-outlet></router-outlet>
  `,
  styles: []
})
export class AppComponent implements OnDestroy {
  private auth = inject(AuthService);
  private stomp = inject(StompService);
  private sub: Subscription;

  constructor() {
    // Connect/disconnect STOMP based on auth state
    this.sub = this.auth.currentUser$.subscribe(user => {
      const token = this.auth.getToken();
      if (user && token) {
        this.stomp.connect(token);
      } else {
        this.stomp.disconnect();
      }
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }
}