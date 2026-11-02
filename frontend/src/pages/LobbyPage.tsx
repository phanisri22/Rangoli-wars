import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, errorMessage } from '../api';
import { BotLevel, RoomSummary, RoomView } from '../types';

export default function LobbyPage() {
  const navigate = useNavigate();
  const [boardSize, setBoardSize] = useState(9);
  const [maxRounds, setMaxRounds] = useState(15);
  const [maxPlayers, setMaxPlayers] = useState(2);
  const [botCount, setBotCount] = useState(1);
  const [botLevel, setBotLevel] = useState<BotLevel>('MEDIUM');
  const [createError, setCreateError] = useState<string | null>(null);
  const [joinCode, setJoinCode] = useState('');
  const [joinError, setJoinError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [openRooms, setOpenRooms] = useState<RoomSummary[]>([]);
  const [listError, setListError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const load = () =>
      api<RoomSummary[]>('/api/game/rooms')
        .then((rooms) => {
          if (!active) return;
          setOpenRooms(rooms);
          setListError(null);
        })
        .catch((e) => active && setListError(errorMessage(e)));
    void load();
    const timer = window.setInterval(load, 5000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    if (botCount > maxPlayers - 1) setBotCount(maxPlayers - 1);
  }, [maxPlayers, botCount]);

  async function create(e: FormEvent) {
    e.preventDefault();
    setCreateError(null);
    setBusy(true);
    try {
      const room = await api<RoomView>('/api/game/rooms', {
        method: 'POST',
        body: { boardSize, maxRounds, maxPlayers, bots: Array(botCount).fill(botLevel) },
      });
      navigate(`/room/${room.code}`);
    } catch (err) {
      setCreateError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function join(code: string, onError: (m: string) => void) {
    const clean = code.trim().toUpperCase();
    if (!clean) {
      onError('Enter a room code');
      return;
    }
    try {
      const room = await api<RoomView>(`/api/game/rooms/${clean}/join`, { method: 'POST' });
      navigate(`/room/${room.code}`);
    } catch (err) {
      onError(errorMessage(err));
    }
  }

  return (
    <div className="lobby">
      <form className="card" onSubmit={create}>
        <h2>New game</h2>
        <div className="grid2">
          <label>
            Board
            <select value={boardSize} onChange={(e) => setBoardSize(Number(e.target.value))}>
              {[7, 9, 11, 13].map((n) => <option key={n} value={n}>{n} × {n}</option>)}
            </select>
          </label>
          <label>
            Rounds
            <select value={maxRounds} onChange={(e) => setMaxRounds(Number(e.target.value))}>
              {[8, 12, 15, 20, 25].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </label>
          <label>
            Players
            <select value={maxPlayers} onChange={(e) => setMaxPlayers(Number(e.target.value))}>
              {[2, 3, 4].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </label>
          <label>
            Bots
            <select value={botCount} onChange={(e) => setBotCount(Number(e.target.value))}>
              {Array.from({ length: maxPlayers }, (_, n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </label>
          <label>
            Bot level
            <select value={botLevel} onChange={(e) => setBotLevel(e.target.value as BotLevel)} disabled={botCount === 0}>
              <option value="EASY">Easy</option>
              <option value="MEDIUM">Medium</option>
              <option value="HARD">Hard</option>
            </select>
          </label>
        </div>
        <div className="actions">
          <button type="submit" className="primary" disabled={busy}>{busy ? 'Creating…' : 'Create room'}</button>
          {createError && <span className="error">{createError}</span>}
        </div>
      </form>

      <form className="card" onSubmit={(e) => { e.preventDefault(); setJoinError(null); void join(joinCode, setJoinError); }}>
        <h2>Join with a code</h2>
        <label>
          Room code
          <input value={joinCode} onChange={(e) => setJoinCode(e.target.value.toUpperCase())} maxLength={5}
                 placeholder="e.g. K7QXP" className="code-input" />
        </label>
        <div className="actions">
          <button type="submit" className="primary">Join</button>
          {joinError && <span className="error">{joinError}</span>}
        </div>

        <h3>Open rooms</h3>
        {listError && <p className="error">{listError}</p>}
        {openRooms.length === 0 && !listError && <p className="muted">No open rooms. Create one and share the code.</p>}
        <ul className="room-list">
          {openRooms.map((r) => (
            <li key={r.code}>
              <span><strong>{r.code}</strong> · host {r.hostId} · {r.players}/{r.maxPlayers} players · {r.boardSize}×{r.boardSize}</span>
              <button type="button" onClick={() => void join(r.code, setListError)}>Join</button>
            </li>
          ))}
        </ul>
      </form>

      <section className="card rules">
        <h2>How to play</h2>
        <ol>
          <li>On your turn, place one tile on any empty cell, or play a power card.</li>
          <li>Close a loop of your tiles to capture every cell inside it, including opponent tiles. Loops cannot use the board edge.</li>
          <li><strong>Splash</strong> colors a 3×3 area. <strong>Shield</strong> protects a tile forever. <strong>Swap</strong> trades one of your tiles with a touching opponent tile.</li>
          <li>You get a new card every 4 turns, up to 3 cards in hand.</li>
          <li>Each turn has a timer. If you run out of time or disconnect, a bot plays for you.</li>
          <li>The player with the most tiles when the rounds run out or the board fills up wins.</li>
        </ol>
      </section>
    </div>
  );
}
