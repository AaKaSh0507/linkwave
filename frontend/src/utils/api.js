import axios from 'axios'

// Create axios instance with default configuration
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  withCredentials: true, // Include cookies in requests (for session and CSRF)
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor to add CSRF token
api.interceptors.request.use(
  (config) => {
    // Get CSRF token from cookie
    const csrfToken = getCsrfTokenFromCookie()
    
    // Add CSRF token to requests that need it (POST, PUT, DELETE)
    if (csrfToken && ['post', 'put', 'delete', 'patch'].includes(config.method?.toLowerCase())) {
      config.headers['X-XSRF-TOKEN'] = csrfToken
    }
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 401 errors handled by component logic
    return Promise.reject(error)
  }
)

// Helper function to get CSRF token from cookie
function getCsrfTokenFromCookie() {
  const name = 'XSRF-TOKEN='
  const decodedCookie = decodeURIComponent(document.cookie)
  const cookies = decodedCookie.split(';')
  
  for (let cookie of cookies) {
    cookie = cookie.trim()
    if (cookie.startsWith(name)) {
      return cookie.substring(name.length)
    }
  }
  
  return null
}

// API endpoints
export const authApi = {
  // Fetch CSRF token (initializes session and sets cookie)
  fetchCsrfToken: () => api.get('/api/v1/auth/csrf'),
  
  // Request OTP for phone number
  requestOtp: (phoneNumber, email) => 
    api.post('/api/v1/auth/request-otp', { phoneNumber, email }),
  
  // Verify OTP
  verifyOtp: (phoneNumber, otp) => 
    api.post('/api/v1/auth/verify-otp', { phoneNumber, otp }),
  
  // Logout
  logout: () => api.post('/api/v1/auth/logout'),
  
  // Get current user
  getCurrentUser: () => api.get('/api/v1/user/me')
}

export default api
