'use client'

import React from "react"

import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { apiCall, setUserPhone, getUserPhone } from './api'
import type { User } from './types'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  requestOTP: (phoneNumber: string, email: string) => Promise<void>
  verifyOTP: (phoneNumber: string, otp: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // Check if user is already authenticated on mount
  useEffect(() => {
    const phone = getUserPhone()

    if (phone) {
      setUser({
        id: phone,
        phoneNumber: phone,
        status: 'online',
      })
    }
    setIsLoading(false)
  }, [])

  const requestOTP = useCallback(async (phoneNumber: string, email: string) => {
    await apiCall<{ message: string }>('/api/v1/auth/request-otp', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber, email }),
    })
  }, [])

  const verifyOTP = useCallback(async (phoneNumber: string, otp: string) => {
    await apiCall<{ authenticated: boolean; message: string }>('/api/v1/auth/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber, otp }),
    })

    setUserPhone(phoneNumber)

    setUser({
      id: phoneNumber,
      phoneNumber,
      status: 'online',
    })
  }, [])

  const logout = useCallback(async () => {
    try {
      await apiCall('/api/v1/auth/logout', { method: 'POST' })
    } catch {
      // Ignore logout API errors
    }
    localStorage.removeItem('user_phone')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        requestOTP,
        verifyOTP,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
