import { DestroyRef, inject, Inject, Service } from '@angular/core';
import { Client, StompSubscription, Versions } from '@stomp/stompjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { StatusUpdateMessage } from '../models/shipment.model';

@Service()
export class WebsocketService {
    private destroyRef = inject(DestroyRef);
    private client!: Client; // définir le client WebSocket
    private connected$ = new BehaviorSubject<boolean>(false);
    private statusUpdate$ = new BehaviorSubject<null | StatusUpdateMessage>(null);
    private subscriptions = new Map<string, StompSubscription>;

    constructor() {
        this.initClient();
        this.destroyRef.onDestroy(() => {
            this.disconnect();
            this.statusUpdate$.complete();
            this.connected$.complete();
        });
    }

    initClient(): void {
        this.client = new Client({
            brokerURL: 'http://localhost:8080/ws',
            stompVersions: Versions.default,
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            connectionTimeout: 10000,

            onConnect: (frame) => {          // frame : c'est ce que le serveur nous renvoie
                console.log("[Websocket] Connected !!");
                this.connected$.next(true);
                this.subscripeToTopic();
            },
            onDisconnect: () => {
                console.log("[Websocket] Disconnected !!");
                this.connected$.next(false);
                this.subscriptions.clear();
            },
            onStompError: (frame) => {
                console.error('[Websocket] STOMP error: ', frame.headers['message']);
                console.error('[Websocket] Error details: ', frame.body);
                this.connected$.next(false);
            },
            onWebSocketError: (event) => {
                console.error('[Websocket] WebSocket error: ', event);
                this.connected$.next(false); 
            },
            onWebSocketClose: (event) => {
                console.error('[Websocket] Websocket closed: ', event.code, event.reason);
                this.connected$.next(false);
            },
        });
    }

    connect(): void {
        if(this.client.active) {
            console.log("[Websocket] Already connected or connecting !!");
            return;
        }
        this.client.activate();
    }

    disconnect(): void {
        if(this.client.active) {
            console.log("[Websocket] Disconnecting...");
            // Unsubscribe from all topics
            this.subscriptions.forEach((subscription) => {
                subscription.unsubscribe();
            });

            // Deactivate the client
            this.client.deactivate();
        }
    }

    getStatusUpdates(): Observable<StatusUpdateMessage | null> {
        return this.statusUpdate$.asObservable();
    } 

    isConnected(): Observable<boolean> {
        return this.connected$.asObservable();
    }

    private subscripeToTopic(): void {
        const subscription = this.client.subscribe("/topic/shipments", (message) => {
            try{
                const update = JSON.parse(message.body) as StatusUpdateMessage;
                console.log('[WebSocket] Received update:', update);
        
                this.statusUpdate$.next(update);
            } catch (error) {
                console.error('[WebSocket] Failed to parse message: ', error);
            }
        });

        this.subscriptions.set('/topic/shipments', subscription);
    }
}
