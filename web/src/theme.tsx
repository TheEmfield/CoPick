import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

export type Theme = 'winter' | 'summer' | 'night' | 'forest';

export const THEME_OPTIONS: { id: Theme; label: string }[] = [
  { id: 'winter', label: 'Зимняя' },
  { id: 'summer', label: 'Летняя' },
  { id: 'night', label: 'Ночная' },
  { id: 'forest', label: 'Лесная' },
];

interface ThemeContextValue {
  theme: Theme;
  setTheme: (t: Theme) => void;
  cycleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

const KEY = 'copick_theme';

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => {
    const stored = localStorage.getItem(KEY) as Theme | null;
    return stored ?? 'winter';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(KEY, theme);
  }, [theme]);

  const setTheme = (t: Theme) => setThemeState(t);

  const cycleTheme = () => {
    setThemeState((prev) => {
      const idx = THEME_OPTIONS.findIndex((t) => t.id === prev);
      return THEME_OPTIONS[(idx + 1) % THEME_OPTIONS.length].id;
    });
  };

  return (
    <ThemeContext.Provider value={{ theme, setTheme, cycleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider');
  return ctx;
}
