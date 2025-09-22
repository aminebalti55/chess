// src/app/features/game/board/board.component.ts
import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MoveSend } from '../game.service';

type Square = string; // 'a1'..'h8'

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './board.component.html',
  styleUrls: ['./board.component.scss']
})
export class BoardComponent {
  @Output() move = new EventEmitter<MoveSend>();

  files = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'];
  ranks = [8, 7, 6, 5, 4, 3, 2, 1];
  selected: Square | null = null;

  squareId(file: string, rank: number): Square {
    return `${file}${rank}`;
  }

  clickSquare(file: string, rank: number) {
    const id = this.squareId(file, rank);

    if (!this.selected) {
      this.selected = id;
      return;
    }

    if (this.selected === id) {
      this.selected = null;
      return;
    }

    // Basic move emission (no client legality; server is source of truth)
    const from = this.selected;
    const to = id;

    // Optional naive promotion prompt if target rank is 8 or 1
    let promotion: 'q' | 'r' | 'b' | 'n' | null = null;
    if (to.endsWith('8') || to.endsWith('1')) {
      const p = prompt('Promotion? q/r/b/n (leave blank for none)');
      if (p && ['q', 'r', 'b', 'n'].includes(p.toLowerCase())) {
        promotion = p.toLowerCase() as any;
      }
    }

    this.move.emit({ from, to, promotion, san: null });
    this.selected = null;
  }

  isDark(file: string, rank: number): boolean {
    // a1 is dark → (file index + rank) odd for dark
    const f = this.files.indexOf(file);
    return (f + rank) % 2 === 1;
  }

  getSquareLabel(file: string, rank: number): string {
    return `${file}${rank}`;
  }
}