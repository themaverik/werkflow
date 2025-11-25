'use client'

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { useSession, signIn, signOut } from 'next-auth/react'

export interface User {
  username: string
  email: string
  firstName?: string
  lastName?: string
  roles: string[]
  groups: string[]
  doaLevel?: number
  department?: string
}

export interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  token: string | null
  login: () => Promise<void>
  logout: () => Promise<void>
  refreshToken: () => Promise<string | null>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { data: session, status } = useSession()
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(status === 'loading')

  // Update user when session changes
  useEffect(() => {
    if (status === 'authenticated' && session) {
      const sessionUser: User = {
        username: session.user?.name || '',
        email: session.user?.email || '',
        firstName: (session as any).user?.given_name,
        lastName: (session as any).user?.family_name,
        roles: (session as any).realm_access?.roles || [],
        groups: (session as any).groups || [],
        doaLevel: (session as any).doa_level,
        department: (session as any).department,
      }
      setUser(sessionUser)
    } else if (status === 'unauthenticated') {
      setUser(null)
    }
    setIsLoading(status === 'loading')
  }, [session, status])

  const login = useCallback(async () => {
    await signIn('keycloak', { redirect: true, callbackUrl: '/portal/tasks' })
  }, [])

  const logout = useCallback(async () => {
    await signOut({ redirect: true, callbackUrl: '/login' })
  }, [])

  const refreshToken = useCallback(async (): Promise<string | null> => {
    // Token refresh is handled by next-auth automatically
    // This is a placeholder for explicit refresh if needed
    return (session as any)?.accessToken || null
  }, [session])

  const value: AuthContextType = {
    user,
    isAuthenticated: status === 'authenticated',
    isLoading,
    token: (session as any)?.accessToken || null,
    login,
    logout,
    refreshToken,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
