import { Client } from '@stomp/stompjs';
import { useCallback, useEffect, useRef, useState } from 'react';
import { api, errorMessage } from './api';
import { useAuth } from './auth';
import { MoveRequest, RoomView } from './types';

/** Loads a room over REST, then keeps it live over STOMP (/topic/rooms/{code}). */
export function useRoom(code: string) {
  const { user } = useAuth();
  const [room, setRoom] = useState<RoomView | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [moveError, setMoveError] = useState<string | null>(null);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  const acceptView = useCallback((next: RoomView) => {
    // a slow REST reply must not overwrite a newer pushed state
    setRoom((prev) => (prev && prev.turnNumber > next.turnNumber ? prev : next));
  }, []);

  useEffect(() => {
    if (!user) return;
    let active = true;
    api<RoomView>(`/api/game/rooms/${code}`)
      .then((view) => active && acceptView(view))
      .catch((e) => active && setLoadError(errorMessage(e)));

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${user.token}` },
      reconnectDelay: 2000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/rooms/${code}`, (msg) => setRoom(JSON.parse(msg.body) as RoomView));
        client.subscribe('/user/queue/errors', (msg) => setMoveError(JSON.parse(msg.body).message));
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => setMoveError(frame.headers.message ?? 'Connection error'),
    });
    client.activate();
    clientRef.current = client;

    return () => {
      active = false;
      clientRef.current = null;
      void client.deactivate();
    };
  }, [code, user, acceptView]);

  const sendMove = useCallback(
    (move: MoveRequest) => {
      setMoveError(null);
      const client = clientRef.current;
      if (!client || !client.connected) {
        setMoveError('Not connected yet, try again in a moment');
        return;
      }
      client.publish({ destination: `/app/rooms/${code}/move`, body: JSON.stringify(move) });
    },
    [code],
  );

  return { room, acceptView, loadError, moveError, setMoveError, connected, sendMove };
}
