// src/app/features/game/game.component.ts
import { Component, inject, OnDestroy } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { GameService, MoveRecord, MoveSend } from './game.service';
import { AuthService } from '../../core/auth/auth.service';
import { AsyncPipe, NgIf, CommonModule } from '@angular/common';
import { Subscription, switchMap, map } from 'rxjs';
import { BoardComponent } from './board/board.component';
import { MovesPanelComponent } from './moves-panel/moves-panel.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [
    NgIf,
    AsyncPipe,
    CommonModule,
    RouterLink,
    BoardComponent,
    MovesPanelComponent,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule
  ],
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.scss']
})
export class GameComponent implements OnDestroy {
  private route = inject(ActivatedRoute);
  private gameService = inject(GameService);
  auth = inject(AuthService);
  private sub = new Subscription();

  gameId!: number;

  moves$ = this.route.paramMap.pipe(
    map(pm => Number(pm.get('id'))),
    switchMap(id => {
      this.gameId = id;
      return this.gameService.combinedMoves$(id);
    })
  );

  // Derive turn (simple heuristic by move count)
  turn$ = this.moves$.pipe(
    map(m => (m.length % 2 === 0 ? 'WHITE' : 'BLACK'))
  );

  onMove(move: MoveSend) {
    this.gameService.sendMove(this.gameId, move);
  }

  ngOnDestroy() {
    this.sub.unsubscribe();
  }
}