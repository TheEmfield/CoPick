import type {
  AuthResponse,
  Profile,
  Genre,
  Preference,
  Group,
  RecommendationResponse,
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
    return request<Profile>('/auth/me', {
      method: 'PATCH',
      body: JSON.stringify(updates),
    });
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
    return request<void>(`/profiles/me/preferences/${genreId}`, {
      method: 'DELETE',
    });
  },

  async deleteAllPreferences() {
    return request<void>('/profiles/me/preferences', { method: 'DELETE' });
  },

  async getGroups() {
    return request<Group[]>('/groups');
  },

  async getGroupDetail(groupId: number) {
    return request<any>(`/groups/${groupId}`);
  },

  async getRecommendations(groupId: number, opts?: {
    types?: string[];
    limit?: number;
    profileIds?: number[];
    genreFilter?: string[];
  }) {
    const params = new URLSearchParams();
    if (opts?.types?.length) params.set('types', opts.types.join(','));
    if (opts?.limit) params.set('limit', String(opts.limit));
    if (opts?.profileIds?.length) params.set('profileIds', opts.profileIds.join(','));
    if (opts?.genreFilter?.length) params.set('genres', opts.genreFilter.join(','));
    
    const query = params.toString();
    return request<any>(`/groups/${groupId}/recommendations${query ? '?' + query : ''}`);
  },

  async createGroup(name: string, ageRestriction: number | null) {
    return request<any>('/groups', {
      method: 'POST',
      body: JSON.stringify({ name, ageRestriction }),
    });
  },

  async joinGroup(inviteCode: string) {
    return request<any>('/groups/join', {
      method: 'POST',
      body: JSON.stringify({ inviteCode }),
    });
  },

  async kickMember(groupId: number, profileId: number) {
    return request<void>(`/groups/${groupId}/members/${profileId}`, {
      method: 'DELETE',
    });
  },

  async leaveGroup(groupId: number) {
    return request<void>(`/groups/${groupId}/leave`, { method: 'POST' });
  },

  async deleteGroup(groupId: number) {
    return request<void>(`/groups/${groupId}`, { method: 'DELETE' });
  },
};