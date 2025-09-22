// src/app/features/lobby/lobby.service.ts
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { StompService } from '../../core/ws/stomp.service';
import { UserLite, OnlineUsersDto, InviteNotification, GameCreated } from '../../core/models/dto';
import { MatDialog } from '@angular/material/dialog';
import { InviteDialogComponent } from './invite-dialog/invite-dialog.component';

export interface InviteDeclined {
  invitationId: string;
  byUser: UserLite;
}

@Injectable({ providedIn: 'root' })
export class LobbyService {
  private stomp = inject(StompService);
  private router = inject(Router);
  private dialog = inject(MatDialog);

  onlineUsers: UserLite[] = [];

  // Stream invites to the component
  readonly invite$ = new Subject<InviteNotification>();
  readonly inviteDeclined$ = new Subject<InviteDeclined>();

  constructor() {
    // PUBLIC online list snapshot
    this.stomp.subscribe<OnlineUsersDto>('/topic/online-users').subscribe(msg => {
      this.onlineUsers = msg.users || [];
      console.log('📥 Received online users:', msg);
    });

    // PERSONAL invitation queue
    this.stomp.subscribe<InviteNotification | InviteDeclined>('/user/queue/invitations').subscribe(msg => {
      if ('fromUser' in msg && msg.fromUser) {
        // This is an InviteNotification
        const invite = msg as InviteNotification;
        console.log('📥 Invite received:', invite);
        this.invite$.next(invite);
      } else if ('byUser' in msg && msg.byUser) {
        // This is an InviteDeclined
        const declined = msg as InviteDeclined;
        console.log('ℹ️ Invite declined notice:', declined);
        this.inviteDeclined$.next(declined);
      } else {
        console.warn('Unknown invitations payload', msg);
      }
    });

    // Game creation notifications
    this.stomp.subscribe<GameCreated>('/user/queue/game-created').subscribe(game => {
      console.log('🎮 Game created:', game);
      this.router.navigate(['/game', game.gameId]);
    });

    // Request snapshot after WS connects
    this.stomp.connected$.subscribe(connected => {
      if (connected) {
        console.log('✅ WebSocket connected, requesting presence snapshot');
        this.requestPresenceSnapshot();
      } else {
        console.log('❌ WebSocket disconnected');
        this.onlineUsers = [];
      }
    });
  }

  requestPresenceSnapshot() {
    this.stomp.send('/app/presence.request', {});
  }

  sendInvite(toUserId: number) {
    console.log('📤 Sending invite to user:', toUserId);
    this.stomp.send('/app/invite.send', { toUserId });
  }

  replyInvite(invitationId: number, accept: boolean) {
    console.log('📤 Replying invite:', invitationId, 'accept=', accept);
    this.stomp.send('/app/invite.reply', { invitationId, accept });
  }
}