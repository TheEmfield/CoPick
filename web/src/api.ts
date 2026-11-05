import type {
  AuthResponse,
  Profile,
  Genre,
  Preference,
  Group,
  GroupDetail,
  RecommendationItem,
  SavedItem,
} from './types';

const BASE_URL = '/api/v1';

export function getToken(): string | null {
  return localStorage.getItem('copick_token');
}

function getHeaders(): HeadersInit {
  const token = getToken();
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${url}`, {
    ...options,
    headers: {
      ...getHeaders(),
      ...options.headers,
    },
  });

  if (res.status === 204) return undefined as unknown as T;

  if (!res.ok) {
    const errorText = await res.text().catch(() => 'Unknown error');
    throw new Error(errorText || `HTTP error! status: ${res.status}`);
  }

  return res.json();
}

export const api = {
  async register(nickname: string, password: string) {
    const response = await request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ nickname, password }),
    });
    localStorage.setItem('copick_token', response.token);
    localStorage.setItem('copick_profile', JSON.stringify(response.profile));
    return response;
  },

  async login(nickname: string, password: string) {
    const response = await request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ nickname, password }),
    });
    localStorage.setItem('copick_token', response.token);
    localStorage.setItem('copick_profile', JSON.stringify(response.profile));
    return response;
  },

  async updateMe(updates: { nickname?: string; password?: string }) {
    const updated = await request<Profile>('/auth/me', {
      method: 'PATCH',
      body: JSON.stringify(updates),
    });
    localStorage.setItem('copick_profile', JSON.stringify(updated));
    return updated;
  },

  logout() {
    localStorage.removeItem('copick_token');
    localStorage.removeItem('copick_profile');
  },

  async getGenres() {
    return request<Genre[]>('/genres');
  },

  async getPreferences() {
    return request<Preference[]>('/profiles/me/preferences');
  },

  async updatePreferences(preferences: { genreId: number; weight: number }[]) {
    return request<Preference[]>('/profiles/me/preferences', {
      method: 'PUT',
      body: JSON.stringify({ preferences }),
    });
  },

  async deletePreference(genreId: number) {
    return request<void>(`/profiles/me/preferences/${genreId}`, { method: 'DELETE' });
  },

  async deleteAllPreferences() {
    return request<void>('/profiles/me/preferences', { method: 'DELETE' });
  },

  async getGroups() {
    return request<Group[]>('/groups');
  },

  async getGroupDetail(groupId: number) {
    return request<GroupDetail>(`/groups/${groupId}`);
  },

  async createGroup(name: string, ageRestriction: number | null) {
    return request<GroupDetail>('/groups', {
      method: 'POST',
      body: JSON.stringify({ name, ageRestriction }),
    });
  },

  async joinGroup(inviteCode: string) {
    return request<GroupDetail>('/groups/join', {
      method: 'POST',
      body: JSON.stringify({ inviteCode }),
    });
  },

  async kickMember(groupId: number, profileId: number) {
    return request<void>(`/groups/${groupId}/members/${profileId}`, { method: 'DELETE' });
  },

  async leaveGroup(groupId: number) {
    return request<void>(`/groups/${groupId}/leave`, { method: 'POST' });
  },

  async deleteGroup(groupId: number) {
    return request<void>(`/groups/${groupId}`, { method: 'DELETE' });
  },

  async refreshInviteCode(groupId: number) {
    const res = await request<{ inviteCode: string }>(`/groups/${groupId}/regenerate-invite`, { method: 'POST' });
    return res.inviteCode;
  },

  async getLastRecommendations(groupId: number) {
    const data = await request<any[]>(`/groups/${groupId}/recommendations/last`);
    return data.map((item: any) => ({
      content: {
        externalId: String(item.title),
        type: 'movie' as const,
        title: item.title || 'Неизвестно',
        genres: item.genres || [],
        year: item.releaseYear || 0,
        rating: item.rating || 0,
        posterUrl: item.posterUrl || 'https://via.placeholder.com/300x450?text=No+Poster',
        overview: item.overview || '',
      },
      score: 0,
      reason: 'Рекомендовано для вашей группы',
    })) as RecommendationItem[];
  },

  async generateRecommendations(
    groupId: number,
    opts?: { genreIds?: number[]; profileIds?: number[]; limit?: number }
  ) {
    const params = new URLSearchParams();
    if (opts?.genreIds?.length) params.set('genreIds', opts.genreIds.join(','));
    if (opts?.profileIds?.length) params.set('profileIds', opts.profileIds.join(','));
    if (opts?.limit) params.set('limit', String(opts.limit));
    const query = params.toString();

    await request<any>(`/groups/${groupId}/recommendations${query ? '?' + query : ''}`);

    const data = await request<any[]>(`/groups/${groupId}/recommendations/last`);
    return data.map((item: any) => ({
      content: {
        externalId: String(item.title),
        type: 'movie' as const,
        title: item.title || 'Неизвестно',
        genres: item.genres || [],
        year: item.releaseYear || 0,
        rating: item.rating || 0,
        posterUrl: item.posterUrl || 'https://via.placeholder.com/300x450?text=No+Poster',
        overview: item.overview || '',
      },
      score: 0,
      reason: 'Рекомендовано для вашей группы',
    })) as RecommendationItem[];
  },

  async getGroupLikes(groupId: number) {
    const data = await request<any[]>(`/groups/${groupId}/saved`);
    return data.map((item: any) => ({
      id: item.savedId,
      content: {
        externalId: item.externalId,
        type: 'movie' as const,
        title: item.title,
        genres: item.genres || [],
        year: item.releaseYear || 0,
        rating: item.rating || 0,
        posterUrl: item.posterUrl || 'https://via.placeholder.com/300x450?text=No+Poster',
        overview: item.overview || '',
      },
      list: 'liked' as const,
      addedAt: item.addedAt,
    }));
  },

  async addGroupLike(groupId: number, content: any) {
    const newItem = await request<any>(`/groups/${groupId}/saved`, {
      method: 'POST',
      body: JSON.stringify({
        externalId: content.externalId || content.title,
        contentType: 'movie',
        title: content.title,
        originalTitle: content.title,
        posterUrl: content.posterUrl,
        backdropUrl: '',
        rating: content.rating,
        releaseYear: content.year,
        overview: content.overview,
        notes: '',
        genres: content.genres,
      }),
    });

    return {
      id: newItem.savedId,
      content: {
        externalId: newItem.externalId,
        type: 'movie' as const,
        title: newItem.title,
        genres: newItem.genres || [],
        year: newItem.releaseYear || 0,
        rating: newItem.rating || 0,
        posterUrl: newItem.posterUrl || 'https://via.placeholder.com/300x450?text=No+Poster',
        overview: newItem.overview || '',
      },
      list: 'liked' as const,
      addedAt: newItem.addedAt,
    };
  },

  async removeGroupLike(groupId: number, savedId: number) {
    return request<void>(`/groups/${groupId}/saved/${savedId}`, {
      method: 'DELETE',
    });
  },
};