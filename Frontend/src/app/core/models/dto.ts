// src/app/core/models/dto.ts
export interface RegisterRequest { 
  email: string; 
  password: string; 
  displayName: string; 
}

export interface LoginRequest { 
  email: string; 
  password: string; 
}

export interface AuthResponse { 
  token: string; 
  userId: number; 
  displayName: string; 
}

export interface CurrentUserDto { 
  id: number; 
  email: string; 
  displayName: string; 
}

export interface UserLite { 
  id: number; 
  displayName: string; 
}

export interface ApiError { 
  message: string; 
  status?: number; 
}

// WebSocket DTOs
export interface OnlineUsersDto {
  users: UserLite[];
}

export interface InviteSend {
  toUserId: number;
}

export interface InviteNotification {
  invitationId: number;
  fromUser: UserLite;
}

export interface InviteReply {
  invitationId: number;
  accept: boolean;
}

export interface GameCreated {
  gameId: number;
  whitePlayerId: number;
  blackPlayerId: number;
}