// src/app/features/game/moves-panel/moves-panel.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MoveRecord } from '../game.service';

@Component({
  selector: 'app-moves-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatDividerModule
  ],
  templateUrl: './moves-panel.component.html',
  styleUrls: ['./moves-panel.component.scss']
})
export class MovesPanelComponent {
  @Input() moves: MoveRecord[] = [];

  getMoveDisplay(move: MoveRecord): string {
    if (move.san) {
      return move.san;
    }
    return `${move.fromSquare} → ${move.toSquare}`;
  }

  formatTime(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}