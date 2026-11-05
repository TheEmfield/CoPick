import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { api, getToken } from './api';
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
    const token = getToken();
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
    await api.login(nickname, password);
    const updatedProfile = await api.updateMe({});
    const storedProfile = localStorage.getItem(PROFILE_KEY);
    if (storedProfile) {
      setProfile(JSON.parse(storedProfile));
    }
  };

  const register = async (nickname: string, password: string) => {
    await api.register(nickname, password);  // ✅ ТОКЕН СОХРАНЯЕТСЯ ВНУТРИ api.register
    const storedProfile = localStorage.getItem(PROFILE_KEY);
    if (storedProfile) {
      setProfile(JSON.parse(storedProfile));
    }
  };

  const updateMe = async (updates: { nickname?: string; password?: string }) => {
    const updated = await api.updateMe(updates);
    setProfile(updated);
    localStorage.setItem(PROFILE_KEY, JSON.stringify(updated));
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