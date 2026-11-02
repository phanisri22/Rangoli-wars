import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, errorMessage } from '../api';
import { MatchDetail, PLAYER_COLORS } from '../types';

export default function MatchPage() {
  const { id } = useParams();
  const [detail, setDetail] = useState<MatchDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<MatchDetail>(`/api/leaderboard/matches/${id}`).then(setDetail).catch((e) => setError(errorMessage(e)));
  }, [id]);

  if (error) return <div className="card narrow"><p className="error">{error}</p><Link to="/leaderboard">Back</Link></div>;
  if (!detail) return <p className="muted center">Loading match…</p>;

  const { match, moves } = detail;
  const nameOf = (color: number) => match.players.find((p) => p.color === color)?.displayName ?? `Player ${color + 1}`;

  return (
    <div className="match">
      <section className="card">
        <div className="room-head">
          <div>
            <span className="muted">Match #{match.id} · room {match.roomCode}</span>
            <h2>{new Date(match.finishedAt).toLocaleString()}</h2>
          </div>
          <a className="button" href={`/api/leaderboard/matches/${match.id}/replay.xml`} download>Download XML replay</a>
        </div>
        <p className="muted">
          {match.boardSize}×{match.boardSize} board · {match.totalMoves} moves{match.endedEarly ? ' · ended early' : ''}
        </p>
        <ul className="scores">
          {[...match.players].sort((a, b) => a.placement - b.placement).map((p) => (
            <li key={p.color}>
              <span className="swatch" style={{ background: PLAYER_COLORS[p.color] }} />
              <span className="name">{p.placement === 1 ? '★ ' : ''}{p.displayName}</span>
              <span className="muted">#{p.placement}</span>
              <strong>{p.score}</strong>
            </li>
          ))}
        </ul>
      </section>

      <section className="card">
        <h3>Moves</h3>
        <table>
          <thead>
            <tr><th>Turn</th><th>Round</th><th>Player</th><th>Move</th><th>Cell</th><th>Gained</th><th>Captured</th></tr>
          </thead>
          <tbody>
            {moves.map((m) => (
              <tr key={m.turn}>
                <td>{m.turn}</td>
                <td>{m.round}</td>
                <td><span className="swatch small" style={{ background: PLAYER_COLORS[m.color] }} /> {nameOf(m.color)}</td>
                <td>{m.type}</td>
                <td>({m.row + 1}, {m.col + 1}){m.row2 !== null && m.col2 !== null ? ` ↔ (${m.row2 + 1}, ${m.col2 + 1})` : ''}</td>
                <td>{m.gained > 0 ? `+${m.gained}` : m.gained}</td>
                <td>{m.captured || ''}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
