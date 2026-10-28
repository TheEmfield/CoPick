const BASE_URL = import.meta.env.DEV ? 'http://localhost:8080/api/v1' : '/api/v1'

const getHeaders = () => {
  const token = localStorage.getItem('token')
  return {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
  }
}

export const authApi = {
  login: async (nickname, password) => {
    const res = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ nickname, password })
    })
    return res.json()
  },
  register: async (nickname, password) => {
    const res = await fetch(`${BASE_URL}/auth/register`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ nickname, password })
    })
    return res.json()
  },
  updateMe: async (data) => {
    const res = await fetch(`${BASE_URL}/auth/me`, {
      method: 'PATCH',
      headers: getHeaders(),
      body: JSON.stringify(data)
    })
    return res.json()
  }
}

export const groupsApi = {
  getAll: async () => {
    const res = await fetch(`${BASE_URL}/groups`, { headers: getHeaders() })
    return res.json()
  },
  getById: async (id) => {
    const res = await fetch(`${BASE_URL}/groups/${id}`, { headers: getHeaders() })
    return res.json()
  },
  create: async (data) => {
    const res = await fetch(`${BASE_URL}/groups`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data)
    })
    return res.json()
  },
  join: async (inviteCode) => {
    const res = await fetch(`${BASE_URL}/groups/join`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ inviteCode })
    })
    return res.json()
  },
  refreshInviteCode: async (id) => {
    const res = await fetch(`${BASE_URL}/groups/${id}/invite`, {
      method: 'POST',
      headers: getHeaders()
    })
    return res.json()
  }
}

export const preferencesApi = {
  getAll: async () => {
    const res = await fetch(`${BASE_URL}/profiles/me/preferences`, { headers: getHeaders() })
    return res.json()
  },
  delete: async (genreId) => {
    await fetch(`${BASE_URL}/profiles/me/preferences/${genreId}`, {
      method: 'DELETE',
      headers: getHeaders()
    })
  }
}

export const recommendationsApi = {
  getForGroup: async (groupId, params = {}) => {
    const query = new URLSearchParams(params).toString()
    const res = await fetch(`${BASE_URL}/groups/${groupId}/recommendations?${query}`, {
      headers: getHeaders()
    })
    return res.json()
  }
}