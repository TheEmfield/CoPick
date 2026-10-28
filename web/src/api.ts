import type {
  AuthResponse,
  Group,
  GroupDetail,
  Preference,
  RecommendationResponse,
  GroupStats,
  SavedItem,
  ContentItem,
} from './types';
import {
  MOCK_GENRES,
  MOCK_GROUP_DETAIL,
  buildRecommendations,
  makeInviteCode,
  MOCK_CONTENT,
} from './mockData';

const DELAY = 450;
const TOKEN_KEY = 'copick_token';
const PROFILE_KEY = 'copick_profile';

function delay<T>(value: T, ms = DELAY): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

function getStore<T>(key: string): T | null {
  const raw = localStorage.getItem(key);
  return raw ? (JSON.parse(raw) as T) : null;
}

function setStore<T>(key: string, value: T): void {
  localStorage.setItem(key, JSON.stringify(value));
}

interface StoredProfile {
  profileId: number;
  nickname: string;
  password: string;
  createdAt: string;
}

function getProfiles(): StoredProfile[] {
  return getStore<StoredProfile[]>('copick_profiles') ?? [];
}
function setProfiles(p: StoredProfile[]): void {
  setStore('copick_profiles', p);
}

function nextProfileId(): number {
  const profiles = getProfiles();
  return profiles.reduce((max, p) => Math.max(max, p.profileId), 0) + 1;
}

function makeToken(profileId: number): string {
  return btoa(`mock.${profileId}.${Date.now()}`);
}

function getGroupsStore(): Group[] {
  return getStore<Group[]>('copick_groups') ?? [];
}
function setGroupsStore(g: Group[]): void {
  setStore('copick_groups', g);
}

function getGroupDetailStore(): Record<number, GroupDetail> {
  return (
    getStore<Record<number, GroupDetail>>('copick_group_details') ?? {
      ...MOCK_GROUP_DETAIL,
    }
  );
}
function setGroupDetailStore(d: Record<number, GroupDetail>): void {
  setStore('copick_group_details', d);
}

function getPrefsStore(): Record<number, Preference[]> {
  return getStore<Record<number, Preference[]>>('copick_prefs') ?? {};
}
function setPrefsStore(p: Record<number, Preference[]>): void {
  setStore('copick_prefs', p);
}

function getSavedStore(): Record<number, SavedItem[]> {
  return getStore<Record<number, SavedItem[]>>('copick_saved') ?? {};
}
function setSavedStore(s: Record<number, SavedItem[]>): void {
  setStore('copick_saved', s);
}

export const api = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },

  getProfileId(): number | null {
    const p = getStore<StoredProfile>(PROFILE_KEY);
    return p?.profileId ?? null;
  },

  async register(nickname: string, password: string): Promise<AuthResponse> {
    const profiles = getProfiles();
    if (profiles.some((p) => p.nickname.toLowerCase() === nickname.toLowerCase())) {
      throw new Error('Никнейм уже занят');
    }
    const profile: StoredProfile = {
      profileId: nextProfileId(),
      nickname,
      password,
      createdAt: new Date().toISOString(),
    };
    profiles.push(profile);
    setProfiles(profiles);
    const token = makeToken(profile.profileId);
    localStorage.setItem(TOKEN_KEY, token);
    setStore(PROFILE_KEY, profile);

    const { profileId } = profile;
    const groups = getGroupsStore();
    const demoOwner: Group = {
      groupId: 1,
      name: 'Пятничный кинозал',
      inviteCode: 'A1B2C3D4',
      memberCount: 4,
      role: 'owner',
    };
    const demoMember: Group = {
      groupId: 2,
      name: 'Семейный вечер',
      inviteCode: 'XYZ12345',
      memberCount: 3,
      role: 'member',
    };
    if (!groups.some((g) => g.groupId === 1)) groups.push(demoOwner);
    if (!groups.some((g) => g.groupId === 2)) groups.push(demoMember);
    setGroupsStore(groups);
    const details = getGroupDetailStore();
    details[1] = details[1] ?? MOCK_GROUP_DETAIL[1];
    details[2] = details[2] ?? MOCK_GROUP_DETAIL[2];
    details[1] = { ...details[1], ownerId: profileId };
    setGroupDetailStore(details);

    return delay({
      token,
      profile: { profileId: profile.profileId, nickname: profile.nickname, createdAt: profile.createdAt },
    });
  },

  async login(nickname: string, password: string): Promise<AuthResponse> {
    const profiles = getProfiles();
    const found = profiles.find(
      (p) => p.nickname.toLowerCase() === nickname.toLowerCase()
    );
    if (!found || found.password !== password) {
      throw new Error('Неверный никнейм или пароль');
    }
    const token = makeToken(found.profileId);
    localStorage.setItem(TOKEN_KEY, token);
    setStore(PROFILE_KEY, found);
    return delay({
      token,
      profile: { profileId: found.profileId, nickname: found.nickname, createdAt: found.createdAt },
    });
  },

  async updateMe(updates: { nickname?: string; password?: string }): Promise<{
    profileId: number;
    nickname: string;
    createdAt: string;
  }> {
    const current = getStore<StoredProfile>(PROFILE_KEY);
    if (!current) throw new Error('Не авторизован');
    const profiles = getProfiles();
    const idx = profiles.findIndex((p) => p.profileId === current.profileId);
    if (idx === -1) throw new Error('Профиль не найден');
    if (updates.nickname) {
      if (
        profiles.some(
          (p) =>
            p.profileId !== current.profileId &&
            p.nickname.toLowerCase() === updates.nickname!.toLowerCase()
        )
      ) {
        throw new Error('Никнейм уже занят');
      }
      profiles[idx].nickname = updates.nickname;
    }
    if (updates.password) {
      profiles[idx].password = updates.password;
    }
    setProfiles(profiles);
    setStore(PROFILE_KEY, profiles[idx]);
    return delay({
      profileId: profiles[idx].profileId,
      nickname: profiles[idx].nickname,
      createdAt: profiles[idx].createdAt,
    });
  },

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(PROFILE_KEY);
  },

  async getGroups(): Promise<Group[]> {
    return delay(getGroupsStore());
  },

  async createGroup(name: string, ageRestriction: number | null): Promise<GroupDetail> {
    const groups = getGroupsStore();
    const details = getGroupDetailStore();
    const profile = getStore<StoredProfile>(PROFILE_KEY)!;
    const id = groups.reduce((max, g) => Math.max(max, g.groupId), 0) + 1;
    const code = makeInviteCode();
    const detail: GroupDetail = {
      groupId: id,
      name,
      inviteCode: code,
      ageRestriction,
      ownerId: profile.profileId,
      createdAt: new Date().toISOString(),
      members: [{ profileId: profile.profileId, nickname: profile.nickname, role: 'owner' }],
    };
    details[id] = detail;
    setGroupDetailStore(details);
    groups.push({
      groupId: id,
      name,
      inviteCode: code,
      memberCount: 1,
      role: 'owner',
    });
    setGroupsStore(groups);
    return delay(detail);
  },

  async joinGroup(inviteCode: string): Promise<GroupDetail> {
    const groups = getGroupsStore();
    const details = getGroupDetailStore();
    const profile = getStore<StoredProfile>(PROFILE_KEY)!;
    const detail = Object.values(details).find(
      (d) => d.inviteCode.toUpperCase() === inviteCode.toUpperCase()
    );
    if (!detail) throw new Error('Группа с таким кодом не найдена');
    if (detail.members.some((m) => m.profileId === profile.profileId)) {
      throw new Error('Вы уже состоите в этой группе');
    }
    detail.members.push({ profileId: profile.profileId, nickname: profile.nickname, role: 'member' });
    details[detail.groupId] = detail;
    setGroupDetailStore(details);
    if (!groups.some((g) => g.groupId === detail.groupId)) {
      groups.push({
        groupId: detail.groupId,
        name: detail.name,
        inviteCode: detail.inviteCode,
        memberCount: detail.members.length,
        role: 'member',
      });
      setGroupsStore(groups);
    }
    return delay(detail);
  },

  async getGroupDetail(groupId: number): Promise<GroupDetail> {
    const details = getGroupDetailStore();
    const detail = details[groupId];
    if (!detail) throw new Error('Группа не найдена');
    return delay(detail);
  },

  async refreshInviteCode(groupId: number): Promise<string> {
    const details = getGroupDetailStore();
    const groups = getGroupsStore();
    if (!details[groupId]) throw new Error('Группа не найдена');
    const code = makeInviteCode();
    details[groupId].inviteCode = code;
    setGroupDetailStore(details);
    const g = groups.find((g) => g.groupId === groupId);
    if (g) {
      g.inviteCode = code;
      setGroupsStore(groups);
    }
    return delay(code, 300);
  },

  async kickMember(groupId: number, profileId: number): Promise<void> {
    const details = getGroupDetailStore();
    const detail = details[groupId];
    if (!detail) throw new Error('Группа не найдена');
    detail.members = detail.members.filter((m) => m.profileId !== profileId);
    setGroupDetailStore(details);
    return delay(undefined);
  },

  async leaveGroup(groupId: number): Promise<void> {
    const groups = getGroupsStore();
    const details = getGroupDetailStore();
    const profile = getStore<StoredProfile>(PROFILE_KEY)!;
    const detail = details[groupId];
    if (detail) {
      detail.members = detail.members.filter((m) => m.profileId !== profile.profileId);
      if (detail.members.length > 0 && detail.ownerId === profile.profileId) {
        detail.ownerId = detail.members[0].profileId;
        detail.members[0] = { ...detail.members[0], role: 'owner' };
      }
      setGroupDetailStore(details);
    }
    setGroupsStore(groups.filter((g) => g.groupId !== groupId));
    return delay(undefined);
  },

  async deleteGroup(groupId: number): Promise<void> {
    const groups = getGroupsStore();
    const details = getGroupDetailStore();
    delete details[groupId];
    setGroupDetailStore(details);
    setGroupsStore(groups.filter((g) => g.groupId !== groupId));
    return delay(undefined);
  },

  async getGenres() {
    return delay(MOCK_GENRES);
  },

  async getPreferences(): Promise<Preference[]> {
    const profile = getStore<StoredProfile>(PROFILE_KEY);
    if (!profile) throw new Error('Не авторизован');
    const prefs = getPrefsStore();
    return delay(prefs[profile.profileId] ?? []);
  },

  async updatePreferences(items: { genreId: number; weight: number }[]): Promise<Preference[]> {
    const profile = getStore<StoredProfile>(PROFILE_KEY)!;
    const prefs = getPrefsStore();
    const genreMap = new Map(MOCK_GENRES.map((g) => [g.genreId, g.genreName]));
    const existing = prefs[profile.profileId] ?? [];
    const map = new Map(existing.map((p) => [p.genreId, p]));
    for (const item of items) {
      const name = genreMap.get(item.genreId) ?? 'unknown';
      map.set(item.genreId, {
        genreId: item.genreId,
        genreName: name,
        weight: item.weight,
        updatedAt: new Date().toISOString(),
      });
    }
    const updated = Array.from(map.values());
    prefs[profile.profileId] = updated;
    setPrefsStore(prefs);
    return delay(updated);
  },

  async deletePreference(genreId: number): Promise<void> {
    const profile = getStore<StoredProfile>(PROFILE_KEY)!;
    const prefs = getPrefsStore();
    prefs[profile.profileId] = (prefs[profile.profileId] ?? []).filter(
      (p) => p.genreId !== genreId
    );
    setPrefsStore(prefs);
    return delay(undefined);
  },

  async deleteAllPreferences(): Promise<void> {
    const profile = getStore<StoredProfile>(PROFILE_KEY)!;
    const prefs = getPrefsStore();
    prefs[profile.profileId] = [];
    setPrefsStore(prefs);
    return delay(undefined);
  },

  async getRecommendations(
    groupId: number,
    opts: {
      types?: string[];
      limit?: number;
      profileIds?: number[];
      mood?: string;
      genreFilter?: string[];
    } = {}
  ): Promise<RecommendationResponse> {
    let genreFilter = opts.genreFilter ?? [];
    if (opts.mood) {
      const moodMap: Record<string, string[]> = {
        chill: ['comedy', 'romance', 'animation'],
        intense: ['action', 'thriller', 'horror'],
        family: ['family', 'animation'],
        brain: ['drama', 'documentary', 'mystery'],
      };
      genreFilter = [...genreFilter, ...(moodMap[opts.mood] ?? [])];
    }
    const items = buildRecommendations({
      types: opts.types,
      limit: opts.limit,
      genreFilter,
      profileIds: opts.profileIds,
    });
    return delay({ items, total: items.length });
  },

  async getGroupStats(groupId: number): Promise<GroupStats> {
    return delay({
      memberCount: 4,
      topGenres: [
        { genreName: 'action', avgWeight: 0.75, minWeight: 0.3 },
        { genreName: 'comedy', avgWeight: 0.6, minWeight: -0.2 },
        { genreName: 'drama', avgWeight: 0.45, minWeight: 0.1 },
      ],
      bannedGenres: ['horror'],
      suggestedMood: 'intense',
    });
  },

  async getSaved(): Promise<SavedItem[]> {
    const profile = getStore<StoredProfile>(PROFILE_KEY)!;
    const saved = getSavedStore();
    return delay(saved[profile.profileId] ?? []);
  },

  async saveItem(content: ContentItem, list: 'watchlist' | 'liked'): Promise<SavedItem> {
    const profile = getStore<StoredProfile>(PROFILE_KEY)!;
    const saved = getSavedStore();
    const list_ = saved[profile.profileId] ?? [];
    const existing = list_.find(
      (s) => s.content.externalId === content.externalId && s.list === list
    );
    if (existing) return delay(existing);
    const item: SavedItem = {
      id: list_.reduce((max, s) => Math.max(max, s.id), 0) + 1,
      content,
      list,
      addedAt: new Date().toISOString(),
    };
    list_.push(item);
    saved[profile.profileId] = list_;
    setSavedStore(saved);
    return delay(item);
  },

  async removeSaved(id: number): Promise<void> {
    const profile = getStore<StoredProfile>(PROFILE_KEY)!;
    const saved = getSavedStore();
    saved[profile.profileId] = (saved[profile.profileId] ?? []).filter(
      (s) => s.id !== id
    );
    setSavedStore(saved);
    return delay(undefined);
  },

  async searchContent(query: string): Promise<ContentItem[]> {
    if (!query.trim()) return delay(MOCK_CONTENT.slice(0, 8));
    const q = query.toLowerCase();
    return delay(
      MOCK_CONTENT.filter((c) => c.title.toLowerCase().includes(q)).slice(0, 12)
    );
  },
};

/*const BASE_URL = '/api/v1'; // Потом вместо моков, когда бэк появится

function getHeaders(): HeadersInit {
  const token = localStorage.getItem('copick_token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

export const api = {
  getToken(): string | null {
    return localStorage.getItem('copick_token');
  },

  async register(nickname: string, password: string) {
    const res = await fetch(`${BASE_URL}/auth/register`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ nickname, password }),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async login(nickname: string, password: string) {
    const res = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ nickname, password }),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async updateMe(updates: { nickname?: string; password?: string }) {
    const res = await fetch(`${BASE_URL}/auth/me`, {
      method: 'PATCH',
      headers: getHeaders(),
      body: JSON.stringify(updates),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  logout(): void {
    localStorage.removeItem('copick_token');
    localStorage.removeItem('copick_profile');
  },

  async getGroups() {
    const res = await fetch(`${BASE_URL}/groups`, { headers: getHeaders() });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async createGroup(name: string, ageRestriction: number | null) {
    const res = await fetch(`${BASE_URL}/groups`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ name, ageRestriction }),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async joinGroup(inviteCode: string) {
    const res = await fetch(`${BASE_URL}/groups/join`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ inviteCode }),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async getGroupDetail(groupId: number) {
    const res = await fetch(`${BASE_URL}/groups/${groupId}`, { headers: getHeaders() });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async refreshInviteCode(groupId: number) {
    const res = await fetch(`${BASE_URL}/groups/${groupId}/invite`, {
      method: 'POST',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async kickMember(groupId: number, profileId: number) {
    const res = await fetch(`${BASE_URL}/groups/${groupId}/members/${profileId}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(await res.text());
  },

  async leaveGroup(groupId: number) {
    const res = await fetch(`${BASE_URL}/groups/${groupId}/leave`, {
      method: 'POST',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(await res.text());
  },

  async deleteGroup(groupId: number) {
    const res = await fetch(`${BASE_URL}/groups/${groupId}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(await res.text());
  },

  async getGenres() {
    const res = await fetch(`${BASE_URL}/genres`, { headers: getHeaders() });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async getPreferences() {
    const res = await fetch(`${BASE_URL}/profiles/me/preferences`, { headers: getHeaders() });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async updatePreferences(items: { genreId: number; weight: number }[]) {
    const res = await fetch(`${BASE_URL}/profiles/me/preferences`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify({ preferences: items }),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async deletePreference(genreId: number) {
    const res = await fetch(`${BASE_URL}/profiles/me/preferences/${genreId}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(await res.text());
  },

  async deleteAllPreferences() {
    const res = await fetch(`${BASE_URL}/profiles/me/preferences`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(await res.text());
  },

  async getRecommendations(
    groupId: number,
    opts: { types?: string[]; limit?: number; profileIds?: number[]; mood?: string; genreFilter?: string[] } = {}
  ) {
    const params = new URLSearchParams();
    if (opts.types?.length) params.set('types', opts.types.join(','));
    if (opts.limit) params.set('limit', String(opts.limit));
    if (opts.profileIds?.length) params.set('profileIds', opts.profileIds.join(','));
    if (opts.mood) params.set('mood', opts.mood);
    if (opts.genreFilter?.length) params.set('genres', opts.genreFilter.join(','));

    const res = await fetch(`${BASE_URL}/groups/${groupId}/recommendations?${params}`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async getGroupStats(groupId: number) {
    const res = await fetch(`${BASE_URL}/groups/${groupId}/recommendations/stats`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async getSaved() {
    const res = await fetch(`${BASE_URL}/profiles/me/saved`, { headers: getHeaders() });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async saveItem(content: ContentItem, list: 'watchlist' | 'liked') {
    const res = await fetch(`${BASE_URL}/profiles/me/saved`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ content, list }),
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async removeSaved(id: number) {
    const res = await fetch(`${BASE_URL}/profiles/me/saved/${id}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error(await res.text());
  },
};*/