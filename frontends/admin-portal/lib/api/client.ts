import axios from 'axios'

// Engine service base URL - supports both Werkflow custom APIs (/werkflow/api/*) and Flowable APIs (/api/*)
const API_BASE_URL = process.env.NEXT_PUBLIC_ENGINE_API_URL || 'http://localhost:8081'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
})

// Token storage for the interceptor
let tokenGetter: (() => Promise<string | null>) | null = null

// Function to set the token getter (called from client components)
export function setApiClientToken(getter: () => Promise<string | null>) {
  tokenGetter = getter
}

// Request interceptor to add authentication token
apiClient.interceptors.request.use(
  async (config) => {
    // Get token from the registered token getter
    if (tokenGetter) {
      try {
        const token = await tokenGetter()
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
      } catch (error) {
        console.error('Error getting auth token:', error)
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // Server responded with error status
      console.error('API Error:', error.response.data)

      if (error.response.status === 401) {
        // Unauthorized - redirect to login
        // In production: window.location.href = '/login'
      }
    } else if (error.request) {
      // Request made but no response received
      console.error('Network Error:', error.request)
    } else {
      // Something else happened
      console.error('Error:', error.message)
    }

    return Promise.reject(error)
  }
)

export default apiClient
