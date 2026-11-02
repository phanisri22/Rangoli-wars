import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, api, errorMessage } from '../api';
import { useAuth } from '../auth';
import { MatchSummary, PlayerStats } from '../types';

export default function LeaderboardPage() {
  const { user } = useAuth();
  const [top, setTop] = useState<PlayerStats[]>([]);
  const [mine, setMine] = useState<PlayerStats | null>(null);
  const [myMatches, setMyMatches] = useState<MatchSummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    api<PlayerStats[]>('/api/leaderboard/top?limit=20').then(setTop).catch((e) => setError(errorMessage(e)));
    api<PlayerStats>(`/api/leaderboard/players/${user.username}`)
      .then(setMine)
      .catch((e) => {
        if (!(e instanceof ApiError && e.status === 404)) setError(errorMessage(e));
      });
    api<MatchSummary[]>(`/api/leaderboard/players/${user.username}/matches?limit=10`)
      .then(setMyMatches)
      .catch(() => setMyMatches([]));
  }, [user]);

  return (
    <div className="leaderboard">
      {error && <p className="error">{error}</p>}
      <section className="card">
        <h2>Your stats</h2>
        {mine ? (
          <div className="stats">
            <div><span className="muted">Rank</span><strong>#{mine.rank}</strong></div>
            <div><span className="muted">Rating</span><strong>{mine.rating}</strong></div>
            <div><span className="muted">Games</span><strong>{mine.gamesPlayed}</strong></div>
            <div><span className="muted">Wins</span><strong>{mine.wins}</strong></div>
            <div><span className="muted">Win rate</span><strong>{Math.round((mine.wins / mine.gamesPlayed) * 100)}%</strong></div>
          </div>
        ) : (
          <p className="muted">Finish a game to get on the board.</p>
        )}
        {myMatches.length > 0 && (
          <>
            <h3>Your recent matches</h3>
            <ul className="room-list">
              {myMatches.map((m) => {
                const result = m.players.find((p) => p.username === user?.username);
                return (
                  <li key={m.id}>
                    <span>{new Date(m.finishedAt).toLocaleString()} · {m.players.length} players · {result?.placement === 1 ? 'won' : `placed ${result?.placement}`} with {result?.score} tiles</span>
                    <Link to={`/match/${m.id}`}>Details</Link>
                  </li>
                );
              })}
            </ul>
          </>
        )}
      </section>

      <section className="card">
        <h2>Top players</h2>
        {top.length === 0 ? (
          <p className="muted">No games recorded yet.</p>
        ) : (
          <table>
            <thead>
              <tr><th>#</th><th>Player</th><th>Rating</th><th>Games</th><th>Wins</th><th>Tiles</th></tr>
            </thead>
            <tbody>
              {top.map((p) => (
                <tr key={p.username} className={p.username === user?.username ? 'me-row' : ''}>
                  <td>{p.rank}</td>
                  <td>{p.username}</td>
                  <td>{p.rating}</td>
                  <td>{p.gamesPlayed}</td>
                  <td>{p.wins}</td>
                  <td>{p.totalTiles}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
