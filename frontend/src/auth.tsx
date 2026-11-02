import { createContext, ReactNode, useCallback, useContext, useState } from 'react';
import { configureApi } from './api';

export interface User {
  username: string;
  token: string;
}

interface AuthState {
  user: User | null;
  login: (user: User) => void;
  logout: () => void;
}

const STORAGE_KEY = 'rangoli.auth';
const AuthContext = createContext<AuthState | null>(null);

function isExpired(token: string) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

function load(): User | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const user = raw ? (JSON.parse(raw) as User) : null;
    return user && !isExpired(user.token) ? user : null;
  } catch {
    return null;
  }
}

function store(user: User | null) {
  try {
    if (user) localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    // storage blocked; the session still works until reload
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(load);

  const login = useCallback((next: User) => {
    store(next);
    setUser(next);
  }, []);

  const logout = useCallback(() => {
    store(null);
    setUser(null);
  }, []);

  configureApi(() => user?.token ?? null, logout);

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
