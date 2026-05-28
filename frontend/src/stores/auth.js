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
      userType: persisted.userType || 'STAFF',
      landingPath: persisted.landingPath || '/',
      linkedCustomerId: persisted.linkedCustomerId || null,
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
    /** Sprint 35: true if SUPER_ADMIN (skip perm checks). */
    isSuperAdmin: (s) => (s.roles || []).some(r => {
      const code = (r?.code || r || '').toUpperCase()
      return code === 'SUPER_ADMIN' || code === 'ROLE_SUPER_ADMIN'
    }),
    /** Sprint 37: customer self-service account (locked to /portal). */
    isCustomer: (s) => s.userType === 'CUSTOMER',
    /** Sprint 37: external partner account. */
    isPartner: (s) => s.userType === 'PARTNER',
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
      this.userType = payload.userType || 'STAFF'
      this.landingPath = payload.landingPath || '/'
      this.linkedCustomerId = payload.linkedCustomerId || null
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
      this.userType = 'STAFF'
      this.landingPath = '/'
      this.linkedCustomerId = null
      localStorage.removeItem(STORAGE_KEY)
    },
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.$state))
    },
  },
})
