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

export interface GroupDetail extends Group {
  ageRestriction: number | null;
  ownerId: number;
  createdAt: string;
  members: GroupMember[];
}

export interface Genre {
  genreId: number;
  genreName: string;
  contentType?: number;
}

export interface Preference {
  genreId: number;
  genreName: string;
  weight: number;
  updatedAt: string;
}

export interface ContentItem {
  externalId: string;
  type: 'movie';
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

export interface SavedItem {
  id: number;
  content: ContentItem;
  list: 'liked';
  addedAt: string;
}