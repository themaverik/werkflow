import axios from 'axios'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
})

// Request interceptor to add authentication token
apiClient.interceptors.request.use(
  async (config) => {
    // Note: In client components, you'll need to pass the token manually
    // In server components, you can use auth() from next-auth
    // For client-side requests, token should be added via context/provider

    // Token will be added from the calling component/page
    // This is just the base configuration
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
