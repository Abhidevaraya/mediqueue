import { Client, StompSubscription, IMessage } from '@stomp/stompjs';
import { tokenStorage } from '@/lib/auth/tokenStorage';
import { QueueRealTimeMessage } from '@/types/queue';

export type ConnectionState = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'ERROR';

interface SubscriptionRecord {
  id: string;
  destination: string;
  callback: (message: QueueRealTimeMessage) => void;
  stompSubscription: StompSubscription | null;
}

export class QueueStompManager {
  private client: Client | null = null;
  private connectionState: ConnectionState = 'DISCONNECTED';
  private stateListeners: Set<(state: ConnectionState) => void> = new Set();
  private subscriptions: Map<string, SubscriptionRecord> = new Map();
  private idCounter = 0;

  private getBrokerUrl(): string {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    try {
      const url = new URL(baseUrl);
      const protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
      return `${protocol}//${url.host}/ws`;
    } catch {
      return 'ws://localhost:8080/ws';
    }
  }

  public getConnectionState(): ConnectionState {
    return this.connectionState;
  }

  public onConnectionStateChange(listener: (state: ConnectionState) => void): () => void {
    this.stateListeners.add(listener);
    listener(this.connectionState);
    return () => {
      this.stateListeners.delete(listener);
    };
  }

  private setConnectionState(newState: ConnectionState) {
    this.connectionState = newState;
    this.stateListeners.forEach((listener) => {
      try {
        listener(newState);
      } catch (err) {
        console.error('Error in STOMP state listener:', err);
      }
    });
  }

  public connect(): Client {
    if (this.client && this.client.active) {
      return this.client;
    }

    const brokerUrl = this.getBrokerUrl();
    const token = tokenStorage.getToken();

    this.setConnectionState('CONNECTING');

    this.client = new Client({
      brokerURL: brokerUrl,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      beforeConnect: () => {
        // Refresh token header on every connect/reconnect attempt
        const freshToken = tokenStorage.getToken();
        if (this.client) {
          this.client.connectHeaders = freshToken ? { Authorization: `Bearer ${freshToken}` } : {};
        }
      },
      onConnect: () => {
        this.setConnectionState('CONNECTED');
        // Activate/restore all registered subscriptions
        this.subscriptions.forEach((record) => {
          this.activateSubscription(record);
        });
      },
      onDisconnect: () => {
        this.setConnectionState('DISCONNECTED');
      },
      onStompError: (frame) => {
        const errorMsg = frame.headers['message'] || '';
        const body = frame.body || '';
        console.error('STOMP broker error:', errorMsg, body);
        this.setConnectionState('ERROR');

        // Check for authentication / authorization failures to prevent infinite reconnection loop
        const combined = `${errorMsg} ${body}`.toLowerCase();
        const isAuthError =
          combined.includes('token') ||
          combined.includes('expired') ||
          combined.includes('revoked') ||
          combined.includes('denied') ||
          combined.includes('unauthorized') ||
          combined.includes('access denied');

        if (isAuthError && this.client) {
          this.client.reconnectDelay = 0;
          try {
            this.client.deactivate();
          } catch {
            // ignore
          }
        }
      },
      onWebSocketClose: () => {
        if (this.connectionState !== 'ERROR') {
          this.setConnectionState('DISCONNECTED');
        }
      },
      onWebSocketError: (event) => {
        console.warn('STOMP WebSocket transport warning:', event);
        this.setConnectionState('ERROR');
      },
    });

    try {
      this.client.activate();
    } catch (err) {
      console.warn('Failed to activate STOMP client:', err);
      this.setConnectionState('ERROR');
    }

    return this.client;
  }

  public disconnect(): void {
    if (this.client) {
      this.subscriptions.forEach((record) => {
        if (record.stompSubscription) {
          try {
            record.stompSubscription.unsubscribe();
          } catch {
            // ignore
          }
          record.stompSubscription = null;
        }
      });
      try {
        this.client.deactivate();
      } catch {
        // ignore
      }
      this.client = null;
      this.setConnectionState('DISCONNECTED');
    }
  }

  private activateSubscription(record: SubscriptionRecord): void {
    if (!this.client || !this.client.connected) return;

    // Remove any stale subscription handle before resubscribing
    if (record.stompSubscription) {
      try {
        record.stompSubscription.unsubscribe();
      } catch {
        // ignore
      }
      record.stompSubscription = null;
    }

    try {
      record.stompSubscription = this.client.subscribe(
        record.destination,
        (msg: IMessage) => {
          try {
            const parsed = JSON.parse(msg.body) as QueueRealTimeMessage;
            record.callback(parsed);
          } catch (e) {
            console.error('Failed to parse queue event JSON:', e);
          }
        }
      );
    } catch (err) {
      console.error('Failed to subscribe to destination:', record.destination, err);
    }
  }

  public subscribe(
    destination: string,
    onMessage: (message: QueueRealTimeMessage) => void
  ): StompSubscription {
    const subId = `sub-${++this.idCounter}-${destination}`;
    const record: SubscriptionRecord = {
      id: subId,
      destination,
      callback: onMessage,
      stompSubscription: null,
    };

    this.subscriptions.set(subId, record);
    const client = this.connect();

    if (client.connected) {
      this.activateSubscription(record);
    }

    return {
      id: subId,
      unsubscribe: () => {
        const existing = this.subscriptions.get(subId);
        if (existing) {
          if (existing.stompSubscription) {
            try {
              existing.stompSubscription.unsubscribe();
            } catch {
              // ignore
            }
          }
          this.subscriptions.delete(subId);
        }
      },
    };
  }

  public subscribeToDoctorQueue(
    doctorId: number,
    onMessage: (message: QueueRealTimeMessage) => void
  ): StompSubscription {
    return this.subscribe(`/topic/doctors/${doctorId}/queue`, onMessage);
  }

  public subscribeToPatientQueue(
    onMessage: (message: QueueRealTimeMessage) => void
  ): StompSubscription {
    return this.subscribe('/user/queue/status', onMessage);
  }
}

export const queueStompManager = new QueueStompManager();
