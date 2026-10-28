import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { Button } from '../components/Button';
import { Film, Palette } from 'lucide-react';
import { useTheme } from '../theme';

export function AuthPage() {
  const { profile, login, register } = useAuth();
  const { theme, cycleTheme } = useTheme();
  const themeLabels: Record<string, string> = {
    winter: 'Зимняя тема',
    summer: 'Летняя тема',
    night: 'Ночная тема',
    forest: 'Лесная тема',
  };
  const navigate = useNavigate();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (profile) navigate('/groups', { replace: true });
  }, [profile, navigate]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!nickname.trim() || !password.trim()) {
      setError('Заполните все поля');
      return;
    }
    if (password.length < 4) {
      setError('Пароль должен быть не короче 4 символов');
      return;
    }
    setLoading(true);
    try {
      if (mode === 'login') {
        await login(nickname.trim(), password);
      } else {
        await register(nickname.trim(), password);
      }
      navigate('/groups');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Что-то пошло не так');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-app flex items-center justify-center p-4">
      <div className="absolute inset-0 opacity-30">
        <div className="absolute top-1/4 left-1/4 h-72 w-72 rounded-full bg-primary blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 h-96 w-96 rounded-full bg-accent blur-3xl" />
      </div>

      <div className="relative w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mb-4 inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-primary text-white shadow-card">
            <Film className="h-8 w-8" />
          </div>
          <h1 className="text-2xl font-bold text-app">CoPick</h1>
          <p className="mt-1 text-sm text-muted">
            Подбираем контент для всей компании
          </p>
        </div>

        <div className="rounded-2xl bg-surface border border-app shadow-card p-6 animate-slide-up">
          <div className="mb-5 flex gap-1 rounded-xl bg-surface-2 p-1">
            {(['login', 'register'] as const).map((m) => (
              <button
                key={m}
                onClick={() => { setMode(m); setError(''); }}
                className={`flex-1 rounded-lg py-2 text-sm font-medium transition-all ${
                  mode === m
                    ? 'bg-surface text-app shadow-soft'
                    : 'text-muted hover:text-app'
                }`}
              >
                {m === 'login' ? 'Вход' : 'Регистрация'}
              </button>
            ))}
          </div>

          <form onSubmit={submit} className="space-y-4">
            <Field
              label="Никнейм"
              value={nickname}
              onChange={setNickname}
              placeholder="Alex"
              autoFocus
            />
            <Field
              label="Пароль"
              type="password"
              value={password}
              onChange={setPassword}
              placeholder="••••••"
            />
            {error && (
              <div className="rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-600">
                {error}
              </div>
            )}
            <Button type="submit" loading={loading} className="w-full">
              {mode === 'login' ? 'Войти' : 'Создать аккаунт'}
            </Button>
          </form>
        </div>

        <div className="mt-4 flex items-center justify-center gap-2">
          <button
            onClick={() => cycleTheme()}
            className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs text-muted hover:text-app transition-colors"
          >
            <Palette className="h-3.5 w-3.5" />
            {themeLabels[theme]}
          </button>
        </div>
      </div>
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  type = 'text',
  placeholder,
  autoFocus,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  placeholder?: string;
  autoFocus?: boolean;
}) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-app">{label}</label>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoFocus={autoFocus}
        className="w-full rounded-xl border border-app bg-surface px-4 py-2.5 text-sm text-app placeholder:text-muted/60 focus:outline-none focus:ring-2 focus:ring-app focus:border-primary transition-all"
      />
    </div>
  );
}
