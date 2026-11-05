// === Аутентификация ===
export interface Profile {
  profileId: number;
  nickname: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  profile: Profile;
}

// === Группы ===
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

export interface GroupDetail extends Group {
  ageRestriction: number | null;
  ownerId: number;
  createdAt: string;
  members: GroupMember[];
}

// === Жанры и предпочтения ===
export interface Genre {
  genreId: number;
  genreName: string;
  contentType?: number; // опционально, пока работаем только с фильмами
}

export interface Preference {
  genreId: number;
  genreName: string;
  weight: number;
  updatedAt: string;
}

// === Контент (пока только фильмы) ===
export interface ContentItem {
  externalId: string;
  type: 'movie'; // жёстко фиксируем, что пока только фильмы
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

// === Статистика группы ===
export interface GroupStats {
  memberCount: number;
  topGenres: { genreName: string; avgWeight: number; minWeight: number }[];
  bannedGenres: string[];
  suggestedMood: string;
}

// === Сохранённое ===
export interface SavedItem {
  id: number;
  content: ContentItem;
  list: 'watchlist' | 'liked';
  addedAt: string;
}