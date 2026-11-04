import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { api } from './api';
import type { Profile } from './types';

interface AuthContextValue {
  profile: Profile | null;
  loading: boolean;
  login: (nickname: string, password: string) => Promise<void>;
  register: (nickname: string, password: string) => Promise<void>;
  updateMe: (updates: { nickname?: string; password?: string }) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const PROFILE_KEY = 'copick_profile';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = api.getToken();
    const stored = localStorage.getItem(PROFILE_KEY);
    if (token && stored) {
      try {
        setProfile(JSON.parse(stored));
      } catch {
        api.logout();
      }
    }
    setLoading(false);
  }, []);

  const login = async (nickname: string, password: string) => {
    const res = await api.login(nickname, password);
    setProfile(res.profile);
  };

  const register = async (nickname: string, password: string) => {
    const res = await api.register(nickname, password);
    setProfile(res.profile);
  };

  const updateMe = async (updates: { nickname?: string; password?: string }) => {
    const updated = await api.updateMe(updates);
    setProfile(updated);
  };

  const logout = () => {
    api.logout();
    setProfile(null);
  };

  return (
    <AuthContext.Provider value={{ profile, loading, login, register, updateMe, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
