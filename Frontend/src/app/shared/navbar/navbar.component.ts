// src/app/shared/navbar/navbar.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { StompService } from '../../core/ws/stomp.service';
import { GameService } from '../../features/game/game.service';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatBadgeModule,
    MatDividerModule,
    MatChipsModule
  ],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent {
  auth = inject(AuthService);
  stomp = inject(StompService);
  gameService = inject(GameService);

  activeGames$ = this.gameService.getActiveGames();

  constructor() {
    // DEBUG: Monitor auth state
    this.auth.currentUser$.subscribe(user => {
      console.log('🔐 Navbar Auth State:', user);
    });
  }

  logout() {
    console.log('🚪 Logout clicked');
    this.auth.logout();
  }
}