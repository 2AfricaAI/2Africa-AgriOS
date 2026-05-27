import { defineStore } from 'pinia'

const STORAGE_KEY = 'af-auth'

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => {
    const persisted = loadFromStorage()
    return {
      accessToken: persisted.accessToken || '',
      refreshToken: persisted.refreshToken || '',
      userId: persisted.userId || null,
      username: persisted.username || '',
      nickname: persisted.nickname || '',
      roles: persisted.roles || [],
      permissions: persisted.permissions || [],
    }
  },
  getters: {
    isLoggedIn: (s) => !!s.accessToken,
    /** True if user's only role is WORKER - they should be locked to /m/ mobile UI */
    isWorkerOnly: (s) => {
      if (!s.roles || s.roles.length === 0) return false
      return s.roles.every(r => {
        const code = (r?.code || r || '').toUpperCase()
        return code === 'WORKER' || code === 'ROLE_WORKER'
      })
    },
  },
  actions: {
    setLogin(payload) {
      this.accessToken = payload.accessToken
      this.refreshToken = payload.refreshToken
      this.userId = payload.userId
      this.username = payload.username
      this.nickname = payload.nickname
      this.roles = payload.roles || []
      this.permissions = payload.permissions || []
      this.persist()
    },
    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.userId = null
      this.username = ''
      this.nickname = ''
      this.roles = []
      this.permissions = []
      localStorage.removeItem(STORAGE_KEY)
    },
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.$state))
    },
  },
})
