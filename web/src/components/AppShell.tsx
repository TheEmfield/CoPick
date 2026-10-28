import { Navigate, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { useTheme } from '../theme';
import { Users, User, LogOut, Palette } from 'lucide-react';

export function AppShell() {
  const { profile, logout } = useAuth();
  const { theme, cycleTheme } = useTheme();
  const navigate = useNavigate();

  if (!profile) return <Navigate to="/login" replace />;

  const themeLabels: Record<string, string> = {
    winter: 'Зима',
    summer: 'Лето',
    night: 'Ночь',
    forest: 'Лес',
  };

  return (
    <div className="min-h-screen bg-app">
      <header className="sticky top-0 z-40 border-b border-app bg-surface/85 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
          <button
            onClick={() => navigate('/about')}
            className="flex items-center gap-2 text-app font-bold text-lg transition-opacity hover:opacity-80"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-primary text-white">
              <FilmIcon />
            </span>
            <span className="hidden sm:inline">CoPick</span>
          </button>

          <nav className="flex items-center gap-1 sm:gap-2">
            <button
              onClick={cycleTheme}
              className="flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-muted hover:bg-surface-2 hover:text-app transition-colors"
              title="Сменить тему"
            >
              <Palette className="h-4 w-4" />
              <span className="hidden sm:inline">{themeLabels[theme]}</span>
            </button>

            <NavBtn icon={<Users className="h-4 w-4" />} label="Группы" onClick={() => navigate('/groups')} />
            <NavBtn icon={<User className="h-4 w-4" />} label={profile.nickname} onClick={() => navigate('/profile')} />
            <button
              onClick={() => { logout(); navigate('/login'); }}
              className="flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-muted hover:bg-surface-2 hover:text-app transition-colors"
              title="Выйти"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </nav>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-6 sm:px-6 sm:py-8">
        <Outlet />
      </main>
    </div>
  );
}

function FilmIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect width="20" height="20" x="2" y="2" rx="2.18" ry="2.18" />
      <line x1="7" y1="2" x2="7" y2="22" />
      <line x1="17" y1="2" x2="17" y2="22" />
      <line x1="2" y1="12" x2="22" y2="12" />
      <line x1="2" y1="7" x2="7" y2="7" />
      <line x1="2" y1="17" x2="7" y2="17" />
      <line x1="17" y1="17" x2="22" y2="17" />
      <line x1="17" y1="7" x2="22" y2="7" />
    </svg>
  );
}

function NavBtn({ icon, label, onClick }: { icon: React.ReactNode; label: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-muted hover:bg-surface-2 hover:text-app transition-colors"
    >
      {icon}
      <span className="hidden sm:inline max-w-[100px] truncate">{label}</span>
    </button>
  );
}
