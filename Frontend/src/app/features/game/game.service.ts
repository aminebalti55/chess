// src/app/features/game/game.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable, Subject, merge, scan, shareReplay } from 'rxjs';
import { StompService } from '../../core/ws/stomp.service';

export interface ActiveGameDto {
  gameId: number;
  youAreWhite: boolean;
  status: 'CREATED' | 'STARTED' | 'FINISHED';
  lastFen?: string | null;
}

export interface MoveSend {
  from: string;
  to: string;
  promotion?: 'q' | 'r' | 'b' | 'n' | null;
  san?: string | null;
}

export interface MoveRecord {
  moveNumber: number;
  fromSquare: string;
  toSquare: string;
  san?: string | null;
  promotion?: string | null;
  playedByUserId: number;
  playedAt: string;
  fenAfter?: string | null;
}

export interface MoveBroadcast {
  moveNumber: number;
  from: string;
  to: string;
  san?: string | null;
  promotion?: string | null;
  by: number;
  ts: string;
}

@Injectable({ providedIn: 'root' })
export class GameService {
  private http = inject(HttpClient);
  private stomp = inject(StompService);

  getActiveGames(): Observable<ActiveGameDto[]> {
    return this.http.get<ActiveGameDto[]>(`${environment.apiUrl}/games/active`);
  }

  getMoves(gameId: number): Observable<MoveRecord[]> {
    return this.http.get<MoveRecord[]>(`${environment.apiUrl}/games/${gameId}/moves`);
  }

  movesStream$(gameId: number): Observable<MoveBroadcast> {
    return this.stomp.subscribe<MoveBroadcast>(`/topic/games/${gameId}`);
  }

  sendMove(gameId: number, move: MoveSend) {
    this.stomp.send(`/app/games/${gameId}/move`, move);
  }

  /**
   * Convenience stream: load history once, then append live moves.
   * Returns a growing array of normalized move records.
   */
  combinedMoves$(gameId: number): Observable<MoveRecord[]> {
    const history$ = this.getMoves(gameId);
    const live$ = this.movesStream$(gameId);

    // Normalize live broadcasts into MoveRecord shape for a single list
    const liveAsRecord$ = new Subject<MoveRecord>();
    live$.subscribe(b => {
      liveAsRecord$.next({
        moveNumber: b.moveNumber,
        fromSquare: b.from,
        toSquare: b.to,
        san: b.san ?? undefined,
        promotion: b.promotion ?? undefined,
        playedByUserId: b.by,
        playedAt: b.ts,
        fenAfter: undefined
      });
    });

    return merge(history$, liveAsRecord$).pipe(
      scan<MoveRecord | MoveRecord[], MoveRecord[]>((acc, val) => {
        if (Array.isArray(val)) return [...val];  // initial history
        return [...acc, val];                      // append live
      }, []),
      shareReplay(1)
    );
  }
}