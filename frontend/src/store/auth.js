import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/utils/api'

export const useAuthStore = defineStore('auth', () => {
  // State
  const phoneNumber = ref(null) // Masked phone number
  const authenticatedAt = ref(null) // ISO timestamp
  const isLoading = ref(false)
  const error = ref(null)

  // Computed
  const isAuthenticated = computed(() => {
    return phoneNumber.value !== null && authenticatedAt.value !== null
  })

  // Actions
  
  /**
   * Initialize CSRF token by calling the /csrf endpoint.
   * This should be called before any authenticated requests.
   */
  async function initializeCsrf() {
    try {
      await authApi.fetchCsrfToken()
    } catch (err) {
      // Non-critical - CSRF token will be set on first request
    }
  }

  /**
   * Request OTP for phone number.
   * Sends OTP to provided email address.
   */
  async function requestOtp(phoneNumber, email) {
    isLoading.value = true
    error.value = null
    
    try {
      await authApi.requestOtp(phoneNumber, email)
      return { success: true }
    } catch (err) {
      const errorMsg = err.response?.data?.error || 'Failed to send OTP. Please try again.'
      error.value = errorMsg
      return { success: false, error: errorMsg }
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Verify OTP and authenticate session.
   * On success, fetches user profile and stores authentication state.
   */
  async function verifyOtp(phoneNumberInput, otp) {
    isLoading.value = true
    error.value = null
    
    try {
      // Verify OTP
      const verifyResponse = await authApi.verifyOtp(phoneNumberInput, otp)
      
      if (verifyResponse.data.authenticated) {
        // Fetch user profile to get authenticated data
        await fetchUserProfile()
        return { success: true }
      } else {
        error.value = 'Verification failed'
        return { success: false, error: 'Verification failed' }
      }
    } catch (err) {
      const errorMsg = err.response?.data?.error || 'Invalid OTP. Please try again.'
      error.value = errorMsg
      return { success: false, error: errorMsg }
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Fetch current user profile from /me endpoint.
   * Used to restore session state on page refresh.
   */
  async function fetchUserProfile() {
    try {
      const response = await authApi.getCurrentUser()
      
      if (response.data) {
        phoneNumber.value = response.data.phoneNumber
        authenticatedAt.value = response.data.authenticatedAt
        return true
      }
      
      return false
    } catch (err) {
      // Not authenticated or session expired
      clearAuth()
      return false
    }
  }

  /**
   * Check if session is valid by calling /me endpoint.
   * Used on app initialization to restore authentication state.
   */
  async function checkSession() {
    isLoading.value = true
    
    try {
      const isValid = await fetchUserProfile()
      return isValid
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Logout - invalidate session on backend and clear local state.
   */
  async function logout() {
    isLoading.value = true
    
    try {
      await authApi.logout()
    } catch (err) {
      // Continue with local logout even if backend call fails
    } finally {
      clearAuth()
      isLoading.value = false
    }
  }

  /**
   * Clear authentication state.
   */
  function clearAuth() {
    phoneNumber.value = null
    authenticatedAt.value = null
    error.value = null
  }

  return {
    // State
    phoneNumber,
    authenticatedAt,
    isLoading,
    error,
    // Computed
    isAuthenticated,
    // Actions
    initializeCsrf,
    requestOtp,
    verifyOtp,
    fetchUserProfile,
    checkSession,
    logout,
    clearAuth
  }
})
