import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, errorMessage } from '../api';
import { useAuth } from '../auth';
import Board from '../components/Board';
import Countdown from '../components/Countdown';
import { BotLevel, CARD_INFO, CardType, MoveType, MoveView, PLAYER_COLORS, PlayerView, RoomView } from '../types';
import { useRoom } from '../useRoom';

function describe(move: MoveView, players: PlayerView[]) {
  const name = players.find((p) => p.id === move.playerId)?.name ?? move.playerId;
  const at = `(${move.row + 1}, ${move.col + 1})`;
  const action = {
    PLACE: `placed a tile at ${at}`,
    SPLASH: `splashed around ${at}`,
    SHIELD: `shielded ${at}`,
    SWAP: `swapped ${at} with (${(move.row2 ?? 0) + 1}, ${(move.col2 ?? 0) + 1})`,
  }[move.type];
  const capture = move.captured > 0 ? ` and captured ${move.captured}!` : '';
  return `${name} ${action}${capture}`;
}

export default function RoomPage() {
  const { code = '' } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const { room, acceptView, loadError, moveError, setMoveError, connected, sendMove } = useRoom(code.toUpperCase());
  const [mode, setMode] = useState<MoveType>('PLACE');
  const [swapFrom, setSwapFrom] = useState<{ row: number; col: number } | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [botLevel, setBotLevel] = useState<BotLevel>('MEDIUM');

  useEffect(() => {
    setMode('PLACE');
    setSwapFrom(null);
  }, [room?.turnNumber]);

  if (loadError && !room) {
    return (
      <div className="card narrow">
        <p className="error">{loadError}</p>
        <Link to="/">Back to lobby</Link>
      </div>
    );
  }
  if (!room) return <p className="muted center">Loading room…</p>;

  const current: RoomView = room;
  const me = current.players.find((p) => p.id === user?.username) ?? null;
  const isHost = current.hostId === user?.username;
  const myTurn = current.phase === 'PLAYING' && me !== null && current.currentPlayerId === me.id;
  const currentPlayer = current.players.find((p) => p.id === current.currentPlayerId);

  async function action(path: string, body?: unknown) {
    setActionError(null);
    try {
      acceptView(await api<RoomView>(`/api/game/rooms/${current.code}/${path}`, { method: 'POST', body }));
    } catch (e) {
      setActionError(errorMessage(e));
    }
  }

  async function leave() {
    setActionError(null);
    try {
      await api(`/api/game/rooms/${current.code}/leave`, { method: 'POST' });
      navigate('/');
    } catch (e) {
      setActionError(errorMessage(e));
    }
  }

  function chooseMode(next: MoveType) {
    setMode(next);
    setSwapFrom(null);
    setMoveError(null);
  }

  function onCell(row: number, col: number) {
    if (!myTurn || !me) return;
    const owner = current.owners[row][col];
    const shielded = current.shields[row][col];
    switch (mode) {
      case 'PLACE':
        if (owner !== -1) {
          setMoveError('That cell is already taken');
          return;
        }
        sendMove({ type: 'PLACE', row, col });
        return;
      case 'SPLASH':
        sendMove({ type: 'SPLASH', row, col });
        return;
      case 'SHIELD':
        if (owner !== me.color || shielded) {
          setMoveError('Pick one of your own unshielded tiles');
          return;
        }
        sendMove({ type: 'SHIELD', row, col });
        return;
      case 'SWAP':
        if (!swapFrom) {
          if (owner !== me.color || shielded) {
            setMoveError('First pick one of your own unshielded tiles');
            return;
          }
          setMoveError(null);
          setSwapFrom({ row, col });
          return;
        }
        if (Math.abs(swapFrom.row - row) + Math.abs(swapFrom.col - col) !== 1 || owner === -1 || owner === me.color) {
          setMoveError('Now pick an opponent tile right next to it');
          return;
        }
        sendMove({ type: 'SWAP', row: swapFrom.row, col: swapFrom.col, row2: row, col2: col });
        setSwapFrom(null);
    }
  }

  const ranked = [...current.players].sort((a, b) => b.score - a.score);

  return (
    <div className="room">
      <div className="room-head">
        <div>
          <span className="muted">Room</span>
          <h2 className="room-code">{current.code}</h2>
        </div>
        <div className="room-meta">
          {current.boardSize}×{current.boardSize} · {current.phase === 'LOBBY' ? `${current.maxRounds} rounds` : `Round ${current.round} of ${current.maxRounds}`}
          <span className={`dot ${connected ? 'on' : 'off'}`} title={connected ? 'Live' : 'Reconnecting'} />
        </div>
      </div>

      {current.phase === 'LOBBY' && (
        <div className="card">
          <p>Share the code <strong>{current.code}</strong> with friends, or add bots. {isHost ? 'Start when everyone is in.' : 'Waiting for the host to start.'}</p>
          <PlayerList room={current} />
          <div className="actions wrap">
            {isHost && (
              <>
                <select value={botLevel} onChange={(e) => setBotLevel(e.target.value as BotLevel)}>
                  <option value="EASY">Easy bot</option>
                  <option value="MEDIUM">Medium bot</option>
                  <option value="HARD">Hard bot</option>
                </select>
                <button type="button" onClick={() => void action('bots', { level: botLevel })}
                        disabled={current.players.length >= current.maxPlayers}>Add bot</button>
                <button type="button" className="primary" onClick={() => void action('start')}>Start game</button>
              </>
            )}
            <button type="button" onClick={() => void leave()}>Leave</button>
            {actionError && <span className="error">{actionError}</span>}
          </div>
        </div>
      )}

      {current.phase !== 'LOBBY' && (
        <div className="play">
          <div className="board-wrap">
            <Board room={current} mode={mode} swapFrom={swapFrom} interactive={myTurn} onCell={onCell} />
          </div>
          <aside className="side">
            {current.phase === 'PLAYING' && (
              <div className="card turn" style={{ borderColor: currentPlayer ? PLAYER_COLORS[currentPlayer.color] : undefined }}>
                {myTurn ? <strong>Your turn</strong> : <span>{currentPlayer?.name}'s turn</span>}
                <Countdown deadline={current.turnDeadline} />
              </div>
            )}

            {current.phase === 'FINISHED' && (
              <div className="card result">
                <h3>
                  {current.winnerIds.length === 0
                    ? 'Room closed'
                    : current.winnerIds.includes(user?.username ?? '')
                      ? current.winnerIds.length > 1 ? 'You tied for the win!' : 'You won!'
                      : `${current.winnerIds.map((id) => current.players.find((p) => p.id === id)?.name ?? id).join(' & ')} won`}
                </h3>
                {current.endedEarly && current.winnerIds.length > 0 && <p className="muted">The game was ended early.</p>}
                {current.matchId
                  ? <Link to={`/match/${current.matchId}`}>View match &amp; replay</Link>
                  : current.winnerIds.length > 0 && <p className="muted">Saving result…</p>}
                <div className="actions"><Link to="/" className="button">Back to lobby</Link></div>
              </div>
            )}

            <div className="card">
              <h3>Scores</h3>
              <ul className="scores">
                {ranked.map((p) => (
                  <li key={p.id} className={p.id === current.currentPlayerId ? 'active' : ''}>
                    <span className="swatch" style={{ background: PLAYER_COLORS[p.color] }} />
                    <span className="name">
                      {p.name}{p.id === me?.id ? ' (you)' : ''}
                      {!p.bot && !p.connected && current.phase === 'PLAYING' && <em className="muted"> · bot playing</em>}
                    </span>
                    <span className="cards-count" title="Cards in hand">{p.cards.length} {p.cards.length === 1 ? 'card' : 'cards'}</span>
                    <strong>{p.score}</strong>
                  </li>
                ))}
              </ul>
            </div>

            {me && current.phase === 'PLAYING' && (
              <div className="card">
                <h3>Your move</h3>
                <div className="hand">
                  <button type="button" className={mode === 'PLACE' ? 'chosen' : ''} onClick={() => chooseMode('PLACE')}>
                    <strong>Tile</strong>
                    <small>Place one tile on an empty cell.</small>
                  </button>
                  {me.cards.map((card: CardType, i) => (
                    <button key={`${card}-${i}`} type="button" className={mode === card ? 'chosen' : ''}
                            onClick={() => chooseMode(card)}>
                      <strong>{CARD_INFO[card].label}</strong>
                      <small>{CARD_INFO[card].help}</small>
                    </button>
                  ))}
                </div>
                {mode === 'SWAP' && myTurn && <p className="hint">{swapFrom ? 'Now pick the opponent tile next to it.' : 'Pick one of your tiles first.'}</p>}
                {moveError && <p className="error">{moveError}</p>}
              </div>
            )}

            {current.lastMove && <p className="muted last-move">{describe(current.lastMove, current.players)}</p>}
          </aside>
        </div>
      )}
    </div>
  );
}

function PlayerList({ room }: { room: RoomView }) {
  return (
    <ul className="scores">
      {room.players.map((p) => (
        <li key={p.id}>
          <span className="swatch" style={{ background: PLAYER_COLORS[p.color] }} />
          <span className="name">{p.name}{p.id === room.hostId ? ' · host' : ''}</span>
        </li>
      ))}
      {Array.from({ length: room.maxPlayers - room.players.length }, (_, i) => (
        <li key={`open-${i}`} className="muted"><span className="swatch empty" /> Open seat</li>
      ))}
    </ul>
  );
}
