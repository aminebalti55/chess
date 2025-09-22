// src/app/core/ws/stomp.service.ts
import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StompService {
  private client?: Client;
  private _connected = new BehaviorSubject<boolean>(false);
  connected$ = this._connected.asObservable();

  connect(token: string) {
    if (this.client?.connected) return;
    
    this.client = new Client({
      brokerURL: environment.wsUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        console.log('🟢 STOMP Connected');
        this._connected.next(true);
      },
      onStompError: (frame) => {
        console.error('❌ STOMP Error:', frame);
        this._connected.next(false);
      },
      onWebSocketClose: () => {
        console.log('🔴 WebSocket Closed');
        this._connected.next(false);
      },
      debug: (str) => {
        // Uncomment for detailed STOMP debugging
        // console.log('STOMP Debug:', str);
      }
    });
    
    this.client.activate();
  }

  disconnect() {
    if (!this.client) return;
    const c = this.client;
    this.client = undefined;
    c.deactivate().finally(() => this._connected.next(false));
  }

  subscribe<T = any>(destination: string): Observable<T> {
    const out$ = new Subject<T>();
    
    const trySub = () => {
      if (!this.client?.connected) { 
        setTimeout(trySub, 300); 
        return; 
      }
      
      console.log('📡 Subscribing to:', destination);
      
      const sub: StompSubscription = this.client.subscribe(destination, (msg: IMessage) => {
        console.log('📨 Message received on', destination, ':', msg.body);
        
        try {
          const parsed = JSON.parse(msg.body) as T;
          out$.next(parsed);
        } catch (e) {
          console.error('Failed to parse message:', e);
          // If parsing fails, pass raw body
          out$.next((msg.body as any) as T);
        }
      });
      
      console.log('✅ Subscribed to:', destination);
      
      // Handle unsubscription on out$.complete
      out$.subscribe({ 
        complete: () => {
          console.log('🔕 Unsubscribing from:', destination);
          sub.unsubscribe();
        }
      });
    };
    
    trySub();
    return out$.asObservable();
  }

  send(destination: string, body: any) {
    if (!this.client?.connected) {
      console.warn('⚠️ Cannot send - not connected. Destination:', destination);
      return;
    }
    
    console.log('📤 Sending to', destination, ':', body);
    this.client.publish({ destination, body: JSON.stringify(body) });
  }
}