import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { useTheme } from '../theme';
import type { Genre, Preference } from '../types';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { useToast } from '../components/Toast';
import { Save, Trash2 } from 'lucide-react';

const WEIGHT_OPTIONS: { value: number; label: string }[] = [
  { value: -1, label: 'Не нравится' },
  { value: -0.5, label: 'Скорее нет' },
  { value: 0, label: 'Нейтрально' },
  { value: 0.5, label: 'Скорее да' },
  { value: 1, label: 'Обожаю' },
];

export function ProfilePage() {
  const { profile } = useAuth();
  const toast = useToast();
  const [genres, setGenres] = useState<Genre[]>([]);
  const [prefs, setPrefs] = useState<Preference[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [g, p] = await Promise.all([api.getGenres(), api.getPreferences()]);
      setGenres(g);
      setPrefs(p);
    } catch {
      toast('Не удалось загрузить данные', 'error');
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const setWeight = (genreId: number, weight: number) => {
    setPrefs((prev: Preference[]) => {
      const map = new Map(prev.map((p: Preference) => [p.genreId, p]));
      const g = genres.find((x: Genre) => x.genreId === genreId);
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
      setPrefs((prev: Preference[]) => prev.filter((p: Preference) => p.genreId !== genreId));
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
        prefs.map((p: Preference) => ({ genreId: p.genreId, weight: p.weight }))
      );
      setPrefs(updated);
      toast('Предпочтения сохранены', 'success');
    } catch {
      toast('Ошибка при сохранении', 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-10">
        <Spinner className="text-primary" />
      </div>
    );
  }

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
          <Button icon={<Save className="h-4 w-4" />} loading={saving} onClick={saveAll}>
            Сохранить
          </Button>
        </div>
      </div>

      <div className="rounded-2xl bg-surface border border-app p-5 shadow-soft">
        <h3 className="mb-4 text-sm font-semibold text-app">Жанры</h3>
        <div className="space-y-3">
          {genres.map((g: Genre) => {
            const pref = prefs.find((p: Preference) => p.genreId === g.genreId);
            const weight = pref?.weight ?? 0;

            return (
              <div key={g.genreId} className="flex items-center gap-3">
                <span className="w-28 shrink-0 text-sm text-app capitalize">
                  {g.genreName}
                </span>
                <div className="flex flex-1 gap-1">
                  {WEIGHT_OPTIONS.map((opt) => (
                    <button
                      key={opt.value}
                      onClick={() => {
                        if (opt.value === 0) {
                          setWeight(g.genreId, 0);
                          if (pref) removePref(g.genreId);
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
    </div>
  );
}