import { config } from './config'

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
  message?: string
}

export async function apiCall<T>(
  endpoint: string,
  options: RequestInit & { method?: string } = {}
): Promise<T> {
  const url = `${config.api.baseUrl}${endpoint}`
  const token = typeof window !== 'undefined' ? localStorage.getItem('auth_token') : null

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers,
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  if (config.features.enableDebugLogging) {
    console.log('[v0] API Call:', { url, method: options.method || 'GET' })
  }

  try {
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), config.api.timeout)

    const response = await fetch(url, {
      ...options,
      headers,
      signal: controller.signal,
    })

    clearTimeout(timeout)

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      if (config.features.enableDebugLogging) {
        console.error('[v0] API Error:', { status: response.status, data: errorData })
      }
      throw new Error(errorData.message || `API Error: ${response.status} ${response.statusText}`)
    }

    const data = await response.json()
    if (config.features.enableDebugLogging) {
      console.log('[v0] API Response:', data)
    }
    return data
  } catch (error) {
    if (config.features.enableDebugLogging) {
      console.error('[v0] API Exception:', error)
    }
    throw error
  }
}

export function getAuthToken(): string | null {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('auth_token')
  }
  return null
}

export function setAuthToken(token: string): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem('auth_token', token)
  }
}

export function clearAuthToken(): void {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('user_id')
    localStorage.removeItem('user_phone')
  }
}

export function getUserId(): string | null {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('user_id')
  }
  return null
}

export function setUserId(userId: string): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem('user_id', userId)
  }
}

export function getUserPhone(): string | null {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('user_phone')
  }
  return null
}

export function setUserPhone(phone: string): void {
  if (typeof window !== 'undefined') {
    localStorage.setItem('user_phone', phone)
  }
}
