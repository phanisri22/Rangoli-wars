import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import AuthPage from './pages/AuthPage';
import LeaderboardPage from './pages/LeaderboardPage';
import LobbyPage from './pages/LobbyPage';
import MatchPage from './pages/MatchPage';
import RoomPage from './pages/RoomPage';

export default function App() {
  const { user, logout } = useAuth();

  if (!user) {
    return (
      <div className="app">
        <AuthPage />
      </div>
    );
  }

  return (
    <div className="app">
      <header className="topbar">
        <NavLink to="/" className="brand">
          <span className="brand-mark" aria-hidden />
          Rangoli Wars
        </NavLink>
        <nav>
          <NavLink to="/" end>Play</NavLink>
          <NavLink to="/leaderboard">Leaderboard</NavLink>
        </nav>
        <div className="me">
          <span>{user.username}</span>
          <button className="link" onClick={logout}>Log out</button>
        </div>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<LobbyPage />} />
          <Route path="/room/:code" element={<RoomPage />} />
          <Route path="/leaderboard" element={<LeaderboardPage />} />
          <Route path="/match/:id" element={<MatchPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
