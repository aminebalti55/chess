// src/app/features/lobby/lobby.component.ts
import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { LobbyService } from './lobby.service';
import { InviteDialogComponent } from './invite-dialog/invite-dialog.component';

@Component({
  selector: 'app-lobby',
  standalone: true,
  imports: [
    CommonModule, 
    MatButtonModule, 
    MatCardModule,
    MatIconModule,
    MatListModule,
    MatChipsModule,
    MatSnackBarModule
  ],
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.scss']
})
export class LobbyComponent implements OnInit, OnDestroy {
  auth = inject(AuthService);
  lobby = inject(LobbyService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private inviteSub?: Subscription;
  private declinedSub?: Subscription;

  ngOnInit(): void {
    console.log('🎮 LobbyComponent initialized');
    
    // Show dialog whenever an invite arrives
    this.inviteSub = this.lobby.invite$.subscribe(invite => {
      console.log('🔔 INVITE RECEIVED IN COMPONENT:', invite);
      console.log('🔔 Opening invite dialog...');
      
      // Show snackbar notification
      this.snackBar.open(
        `${invite.fromUser.displayName} invited you to play!`, 
        'VIEW',
        { duration: 5000 }
      );
      
      const dialogRef = this.dialog.open(InviteDialogComponent, {
        width: '400px',
        data: invite,
        disableClose: true
      });

      console.log('🔔 Dialog opened successfully');

      dialogRef.afterClosed().subscribe(accept => {
        console.log('🔔 Dialog closed with result:', accept);
        if (typeof accept === 'boolean') {
          this.lobby.replyInvite(invite.invitationId, accept);
          
          if (accept) {
            this.snackBar.open('Invitation accepted! Starting game...', 'OK', { duration: 3000 });
          } else {
            this.snackBar.open('Invitation declined', 'OK', { duration: 3000 });
          }
        }
      });
    });

    // Handle declined notifications
    this.declinedSub = this.lobby.inviteDeclined$.subscribe(declined => {
      console.log('📭 Invite was declined:', declined);
      this.snackBar.open(
        `${declined.byUser.displayName} declined your invitation`, 
        'OK',
        { duration: 4000 }
      );
    });

    // Request snapshot when entering lobby
    this.lobby.requestPresenceSnapshot();
  }

  ngOnDestroy(): void {
    console.log('🎮 LobbyComponent destroyed');
    this.inviteSub?.unsubscribe();
    this.declinedSub?.unsubscribe();
  }

  get users() {
    return this.lobby.onlineUsers;
  }

  invite(userId: number) {
    console.log('📤 Inviting user:', userId);
    this.lobby.sendInvite(userId);
    this.snackBar.open('Invitation sent!', 'OK', { duration: 2000 });
  }
}