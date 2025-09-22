// src/app/features/lobby/invite-dialog/invite-dialog.component.ts
import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { InviteNotification } from '../../../core/models/dto';

@Component({
  selector: 'app-invite-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './invite-dialog.component.html',
  styleUrls: ['./invite-dialog.component.scss']
})
export class InviteDialogComponent {
  data = inject(MAT_DIALOG_DATA) as InviteNotification;
  ref = inject(MatDialogRef<InviteDialogComponent>);

  constructor() {
    console.log('🎨 Dialog opened with data:', this.data);
  }

  accept() { 
    console.log('✅ User accepted invitation:', this.data.invitationId);
    this.ref.close(true); 
  }

  decline() { 
    console.log('❌ User declined invitation:', this.data.invitationId);
    this.ref.close(false); 
  }
}