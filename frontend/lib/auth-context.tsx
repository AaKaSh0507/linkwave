'use client'

import React from "react"

import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { apiCall, setAuthToken, getAuthToken, getUserId, setUserId, setUserPhone } from './api'
import { config } from './config'
import type { User, AuthResponse } from './types'

interface AuthContextType {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  requestOTP: (phoneNumber: string) => Promise<{ otpId: string }>
  verifyOTP: (otpId: string, otp: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // Check if user is already authenticated on mount
  useEffect(() => {
    const token = getAuthToken()
    const userId = getUserId()

    if (token && userId) {
      // Could fetch user details here
      setUser({
        id: userId,
        phoneNumber: localStorage.getItem('user_phone') || '',
        status: 'online',
      })
    }
    setIsLoading(false)
  }, [])

  const requestOTP = useCallback(async (phoneNumber: string) => {
    if (config.features.enableDebugLogging) {
      console.log('[v0] Requesting OTP for:', phoneNumber)
    }
    const response = await apiCall<{ otpId: string }>('/auth/request-otp', {
      method: 'POST',
      body: JSON.stringify({ phoneNumber }),
    })
    if (config.features.enableDebugLogging) {
      console.log('[v0] OTP requested, ID:', response.otpId)
    }
    return response
  }, [])

  const verifyOTP = useCallback(async (otpId: string, otp: string) => {
    if (config.features.enableDebugLogging) {
      console.log('[v0] Verifying OTP for ID:', otpId)
    }
    const response = await apiCall<AuthResponse>('/auth/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ otpId, otp }),
    })

    setAuthToken(response.token)
    setUserId(response.user.id)
    setUserPhone(response.user.phoneNumber)

    setUser({
      id: response.user.id,
      phoneNumber: response.user.phoneNumber,
      displayName: response.user.displayName,
      status: 'online',
    })

    if (config.features.enableDebugLogging) {
      console.log('[v0] User authenticated:', response.user.id)
    }
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('user_id')
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
