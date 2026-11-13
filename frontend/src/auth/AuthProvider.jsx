import { useCallback, useEffect, useMemo, useState } from 'react'
import AuthContext from './auth-context'
import api, { responseData, SESSION_KEY } from '../lib/api'

function readSession() {
  const raw = sessionStorage.getItem(SESSION_KEY)
  if (!raw) return null
  try {
    const session = JSON.parse(raw)
    if (!session?.accessToken || !session?.user || session.expiresAt <= Date.now()) {
      sessionStorage.removeItem(SESSION_KEY)
      return null
    }
    return session
  } catch {
    sessionStorage.removeItem(SESSION_KEY)
    return null
  }
}

export default function AuthProvider({ children }) {
  const [session, setSession] = useState(readSession)

  const clearSession = useCallback(() => {
    sessionStorage.removeItem(SESSION_KEY)
    setSession(null)
  }, [])

  useEffect(() => {
    window.addEventListener('trainreservation:session-expired', clearSession)
    return () => window.removeEventListener('trainreservation:session-expired', clearSession)
  }, [clearSession])

  const login = useCallback(async (credentials) => {
    const auth = responseData(await api.post('/api/auth/login', credentials))
    const nextSession = {
      accessToken: auth.accessToken,
      tokenType: auth.tokenType || 'Bearer',
      user: auth.user,
      expiresAt: Date.now() + Number(auth.expiresInSeconds) * 1000,
    }
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(nextSession))
    setSession(nextSession)
    return auth.user
  }, [])

  const value = useMemo(() => ({
    user: session?.user ?? null,
    isAuthenticated: Boolean(session?.accessToken),
    login,
    logout: clearSession,
  }), [clearSession, login, session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
