import { FormEvent, useState } from 'react';
import { api, errorMessage } from '../api';
import { useAuth } from '../auth';

interface AuthResponse {
  token: string;
  username: string;
}

export default function AuthPage() {
  const { login } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const res = await api<AuthResponse>(`/api/auth/${mode}`, { method: 'POST', body: { username, password } });
      login({ username: res.username, token: res.token });
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <div className="auth-hero">
        <h1>Rangoli Wars</h1>
        <p>Paint the board in your color. Close a loop of tiles to capture everything inside it, and use your power cards at the right moment.</p>
      </div>
      <form className="card auth-card" onSubmit={submit}>
        <div className="tabs">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Log in</button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Create account</button>
        </div>
        <label>
          Username
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 autoComplete={mode === 'login' ? 'current-password' : 'new-password'} required />
        </label>
        {mode === 'register' && <p className="hint">3–20 letters, digits or _. Password at least 6 characters.</p>}
        <div className="actions">
          <button type="submit" className="primary" disabled={busy}>
            {busy ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Create account'}
          </button>
          {error && <span className="error">{error}</span>}
        </div>
      </form>
    </div>
  );
}
