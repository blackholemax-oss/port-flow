import { defineStore } from 'pinia'
import { ref } from 'vue'

import { authApi } from '@/api'

const STORAGE_KEY = 'pf-admin-user'

interface SavedUser {
  email?: string
  userId?: number
}

function loadSaved(): SavedUser {
  try {
    return (JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') as SavedUser | null) ?? {}
  } catch {
    return {}
  }
}

export const useAuthStore = defineStore('auth', () => {
  const saved = loadSaved()
  const email = ref<string>(saved.email ?? '')
  const userId = ref<number | null>(saved.userId ?? null)

  function persist() {
    if (email.value) {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ email: email.value, userId: userId.value }),
      )
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  async function login(loginEmail: string, password: string) {
    const res = await authApi.login(loginEmail, password)
    email.value = res.email
    userId.value = res.userId
    persist()
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      email.value = ''
      userId.value = null
      persist()
    }
  }

  const isLoggedIn = () => !!email.value

  return { email, userId, login, logout, isLoggedIn }
})
