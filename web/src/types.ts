export type ContentType = 1 | 2 | 3;

export const CONTENT_TYPE_LABEL: Record<ContentType, string> = {
  1: 'Фильмы',
  2: 'Сериалы',
  3: 'Игры',
};

export interface Profile {
  profileId: number;
  nickname: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  profile: Profile;
}

export interface Group {
  groupId: number;
  name: string;
  inviteCode: string;
  memberCount: number;
  role: 'owner' | 'member';
}

export interface GroupMember {
  profileId: number;
  nickname: string;
  role: 'owner' | 'member';
}

export interface GroupDetail {
  groupId: number;
  name: string;
  inviteCode: string;
  ageRestriction: number | null;
  ownerId: number;
  createdAt: string;
  members: GroupMember[];
}

export interface Genre {
  genreId: number;
  genreName: string;
  contentType: ContentType;
}

export interface Preference {
  genreId: number;
  genreName: string;
  weight: number;
  updatedAt: string;
}

export interface ContentItem {
  externalId: string;
  type: 'movie' | 'series' | 'game';
  title: string;
  genres: string[];
  year: number;
  rating: number;
  posterUrl: string;
  overview: string;
}

export interface RecommendationItem {
  content: ContentItem;
  score: number;
  reason: string;
}

export interface RecommendationResponse {
  items: RecommendationItem[];
  total: number;
}

export interface GroupStats {
  memberCount: number;
  topGenres: { genreName: string; avgWeight: number; minWeight: number }[];
  bannedGenres: string[];
  suggestedMood: string;
}

export type Mood = 'chill' | 'intense' | 'family' | 'brain';

export const MOOD_PRESETS: { id: Mood; label: string; description: string; genres: string[] }[] = [
  { id: 'chill', label: 'Чилл', description: 'Комедии, романтика, анимация', genres: ['comedy', 'romance', 'animation'] },
  { id: 'intense', label: 'Драйв', description: 'Экшен, триллеры, хоррор', genres: ['action', 'thriller', 'horror'] },
  { id: 'family', label: 'Семейный', description: 'Семейные, анимация, мягкий фильтр', genres: ['family', 'animation'] },
  { id: 'brain', label: 'Для ума', description: 'Драмы, документалки, детективы', genres: ['drama', 'documentary', 'mystery'] },
];

export interface SavedItem {
  id: number;
  content: ContentItem;
  list: 'watchlist' | 'liked';
  addedAt: string;
}
