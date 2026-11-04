import { useEffect, useState, useCallback } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { useTheme } from '../theme';
import type { Genre, Preference, ContentType } from '../types';
import { CONTENT_TYPE_LABEL } from '../types';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { useToast } from '../components/Toast';
import {
  User, Palette, Sliders, Trash2, Save,
  Snowflake, Sun, Moon, Trees, Check,
} from 'lucide-react';

type Tab = 'account' | 'preferences' | 'theme';

const WEIGHT_OPTIONS: { value: number; label: string }[] = [
  { value: -1, label: 'Не нравится' },
  { value: -0.5, label: 'Скорее нет' },
  { value: 0, label: 'Нейтрально' },
  { value: 0.5, label: 'Скорее да' },
  { value: 1, label: 'Обожаю' },
];

export function ProfilePage() {
  const [tab, setTab] = useState<Tab>('account');

  const tabs: { id: Tab; label: string; icon: typeof User }[] = [
    { id: 'account', label: 'Аккаунт', icon: User },
    { id: 'preferences', label: 'Предпочтения', icon: Sliders },
    { id: 'theme', label: 'Тема', icon: Palette },
  ];

  return (
    <div className="animate-fade-in">
      <h1 className="mb-6 text-2xl font-bold text-app">Профиль</h1>

      <div className="grid gap-6 lg:grid-cols-[200px_1fr]">
        <nav className="flex gap-2 lg:flex-col overflow-x-auto">
          {tabs.map((t) => {
            const Icon = t.icon;
            return (
              <button
                key={t.id}
                onClick={() => setTab(t.id)}
                className={`flex items-center gap-2.5 rounded-xl px-4 py-2.5 text-sm font-medium whitespace-nowrap transition-all ${
                  tab === t.id
                    ? 'bg-primary text-white shadow-soft'
                    : 'text-muted hover:bg-surface-2 hover:text-app'
                }`}
              >
                <Icon className="h-4 w-4" />
                {t.label}
              </button>
            );
          })}
        </nav>

        <div className="min-w-0">
          {tab === 'account' && <AccountTab />}
          {tab === 'preferences' && <PreferencesTab />}
          {tab === 'theme' && <ThemeTab />}
        </div>
      </div>
    </div>
  );
}

function AccountTab() {
  const { profile, updateMe } = useAuth();
  const toast = useToast();
  const [nickname, setNickname] = useState(profile?.nickname ?? '');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    const updates: { nickname?: string; password?: string } = {};
    if (nickname.trim() && nickname !== profile?.nickname) updates.nickname = nickname.trim();
    if (password) updates.password = password;
    if (Object.keys(updates).length === 0) {
      toast('Нет изменений', 'info');
      return;
    }
    setLoading(true);
    try {
      await updateMe(updates);
      setPassword('');
      toast('Данные обновлены', 'success');
    } catch (err) {
      toast(err instanceof Error ? err.message : 'Ошибка', 'error');
    } finally {
      setLoading(false);
    }
  };

  if (!profile) return null;

  return (
    <div className="max-w-md rounded-2xl bg-surface border border-app p-6 shadow-soft">
      <div className="mb-6 flex items-center gap-4">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-soft text-primary text-xl font-bold">
          {profile.nickname.slice(0, 2).toUpperCase()}
        </div>
        <div>
          <p className="font-semibold text-app text-lg">{profile.nickname}</p>
          <p className="text-sm text-muted">
            С {new Date(profile.createdAt).toLocaleDateString('ru-RU')}
          </p>
        </div>
      </div>

      <form onSubmit={save} className="space-y-4">
        <div>
          <label className="mb-1.5 block text-sm font-medium text-app">Никнейм</label>
          <input
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            className="w-full rounded-xl border border-app bg-surface px-4 py-2.5 text-sm text-app focus:outline-none focus:ring-2 focus:ring-app focus:border-primary"
          />
        </div>
        <div>
          <label className="mb-1.5 block text-sm font-medium text-app">Новый пароль</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Оставьте пустым, чтобы не менять"
            className="w-full rounded-xl border border-app bg-surface px-4 py-2.5 text-sm text-app placeholder:text-muted/60 focus:outline-none focus:ring-2 focus:ring-app focus:border-primary"
          />
        </div>
        <Button type="submit" loading={loading} icon={<Save className="h-4 w-4" />}>
          Сохранить
        </Button>
      </form>
    </div>
  );
}

function PreferencesTab() {
  const [genres, setGenres] = useState<Genre[]>([]);
  const [prefs, setPrefs] = useState<Preference[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const toast = useToast();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [g, p] = await Promise.all([api.getGenres(), api.getPreferences()]);
      setGenres(g);
      setPrefs(p);
    } catch {
      toast('Не удалось загрузить', 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => { load(); }, [load]);

  const prefMap = new Map(prefs.map((p) => [p.genreId, p.weight]));

  const setWeight = (genreId: number, weight: number) => {
    setPrefs((prev) => {
      const map = new Map(prev.map((p) => [p.genreId, p]));
      const g = genres.find((x) => x.genreId === genreId);
      map.set(genreId, {
        genreId,
        genreName: g?.genreName ?? '',
        weight,
        updatedAt: new Date().toISOString(),
      });
      return Array.from(map.values());
    });
  };

  const removePref = async (genreId: number) => {
    try {
      await api.deletePreference(genreId);
      setPrefs((prev) => prev.filter((p) => p.genreId !== genreId));
      toast('Удалено', 'info');
    } catch {
      toast('Ошибка', 'error');
    }
  };

  const clearAll = async () => {
    try {
      await api.deleteAllPreferences();
      setPrefs([]);
      toast('Все предпочтения очищены', 'info');
    } catch {
      toast('Ошибка', 'error');
    }
  };

  const saveAll = async () => {
    setSaving(true);
    try {
      const updated = await api.updatePreferences(
        prefs.map((p) => ({ genreId: p.genreId, weight: p.weight }))
      );
      setPrefs(updated);
      toast('Предпочтения сохранены', 'success');
    } catch {
      toast('Ошибка при сохранении', 'error');
    } finally {
      setSaving(false);
    }
  };

  const hasChanges = true;

  if (loading) {
    return <div className="flex justify-center py-10"><Spinner className="text-primary" /></div>;
  }

  const grouped: Record<ContentType, Genre[]> = { 1: [], 2: [], 3: [] };
  for (const g of genres) grouped[g.contentType].push(g);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted">
          Выберите отношение к жанру: от «не нравится» до «обожаю»
        </p>
        <div className="flex gap-2">
          <Button variant="secondary" icon={<Trash2 className="h-4 w-4" />} onClick={clearAll}>
            Очистить
          </Button>
          <Button icon={<Save className="h-4 w-4" />} loading={saving} onClick={saveAll} disabled={!hasChanges}>
            Сохранить
          </Button>
        </div>
      </div>

      {(Object.keys(grouped) as unknown as ContentType[]).map((ct) => {
        const items = grouped[ct];
        if (items.length === 0) return null;
        return (
          <div key={ct} className="rounded-2xl bg-surface border border-app p-5 shadow-soft">
            <h3 className="mb-4 text-sm font-semibold text-app">{CONTENT_TYPE_LABEL[ct]}</h3>
            <div className="space-y-3">
              {items.map((g) => {
                const weight = prefMap.get(g.genreId) ?? 0;
                return (
                  <div key={g.genreId} className="flex items-center gap-3">
                    <span className="w-28 shrink-0 text-sm text-app capitalize">{g.genreName}</span>
                    <div className="flex flex-1 gap-1">
                      {WEIGHT_OPTIONS.map((opt) => (
                        <button
                          key={opt.value}
                          onClick={() => {
                            if (opt.value === 0) {
                              setWeight(g.genreId, 0);
                              if (prefMap.has(g.genreId)) removePref(g.genreId);
                            } else {
                              setWeight(g.genreId, opt.value);
                            }
                          }}
                          className={`flex-1 rounded-lg py-1.5 text-xs font-medium transition-all ${
                            weight === opt.value
                              ? opt.value > 0
                                ? 'bg-primary text-white shadow-soft'
                                : opt.value < 0
                                  ? 'bg-red-500 text-white shadow-soft'
                                  : 'bg-surface-2 text-app'
                              : 'bg-surface-2 text-muted hover:text-app'
                          }`}
                        >
                          {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function ThemeTab() {
  const { theme, setTheme } = useTheme();
  type ThemeId = 'winter' | 'summer' | 'night' | 'forest';

  const themes: {
    id: ThemeId;
    label: string;
    description: string;
    icon: typeof Snowflake;
    preview: { bg: string; surface: string; primary: string; accent: string; text: string };
  }[] = [
    {
      id: 'winter',
      label: 'Зимняя',
      description: 'Холодные, спокойные тона',
      icon: Snowflake,
      preview: { bg: '#eaf6ff', surface: '#ffffff', primary: '#0ea5e9', accent: '#38bdf8', text: '#0f233c' },
    },
    {
      id: 'summer',
      label: 'Летняя',
      description: 'Тёплые, солнечные тона',
      icon: Sun,
      preview: { bg: '#fff7e0', surface: '#fffcf0', primary: '#ea580c', accent: '#e9a24a', text: '#3c240c' },
    },
    {
      id: 'night',
      label: 'Ночная',
      description: 'Тёмная, приглушённая гамма',
      icon: Moon,
      preview: { bg: '#0e1014', surface: '#181b21', primary: '#60a5fa', accent: '#94a3b8', text: '#e2e6ec' },
    },
    {
      id: 'forest',
      label: 'Лесная',
      description: 'Природные, зелёные оттенки',
      icon: Trees,
      preview: { bg: '#f0f9ee', surface: '#ffffff', primary: '#16a34a', accent: '#84cc16', text: '#142d1e' },
    },
  ];

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {themes.map((t) => {
        const Icon = t.icon;
        const active = theme === t.id;
        return (
          <button
            key={t.id}
            onClick={() => setTheme(t.id)}
            className={`relative overflow-hidden rounded-2xl border-2 p-5 text-left transition-all ${
              active ? 'border-primary shadow-card' : 'border-app hover:border-primary/50'
            }`}
          >
            <div
              className="mb-4 flex h-28 items-center justify-center rounded-xl"
              style={{ backgroundColor: t.preview.bg }}
            >
              <div className="flex items-center gap-2">
                <div
                  className="flex h-10 w-10 items-center justify-center rounded-xl"
                  style={{ backgroundColor: t.preview.primary, color: '#fff' }}
                >
                  <Icon className="h-5 w-5" />
                </div>
                <div className="flex flex-col gap-1">
                  <div className="h-2 w-16 rounded-full" style={{ backgroundColor: t.preview.text }} />
                  <div className="h-2 w-10 rounded-full opacity-50" style={{ backgroundColor: t.preview.text }} />
                </div>
                <div className="h-6 w-6 rounded-full" style={{ backgroundColor: t.preview.accent }} />
              </div>
            </div>
            <div className="flex items-center justify-between">
              <div>
                <p className="font-semibold text-app">{t.label}</p>
                <p className="text-sm text-muted">{t.description}</p>
              </div>
              {active && (
                <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-white">
                  <Check className="h-4 w-4" />
                </div>
              )}
            </div>
          </button>
        );
      })}
    </div>
  );
}
