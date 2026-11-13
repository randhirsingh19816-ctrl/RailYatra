import axios from 'axios'

export const SESSION_KEY = 'trainreservation.session'

const api = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, ''),
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const rawSession = sessionStorage.getItem(SESSION_KEY)
  if (rawSession) {
    try {
      const session = JSON.parse(rawSession)
      if (session?.accessToken) {
        config.headers.Authorization = `${session.tokenType || 'Bearer'} ${session.accessToken}`
      }
    } catch {
      sessionStorage.removeItem(SESSION_KEY)
    }
  }
  return config
})

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401 && sessionStorage.getItem(SESSION_KEY)) {
      sessionStorage.removeItem(SESSION_KEY)
      window.dispatchEvent(new Event('trainreservation:session-expired'))
    }
    return Promise.reject(error)
  },
)

export function responseData(envelope) {
  return envelope?.data
}

export function errorMessage(error) {
  const body = error?.response?.data
  if (typeof body?.message === 'string' && body.message.trim()) return body.message
  if (error?.code === 'ECONNABORTED') return 'The server took too long to respond. Please try again.'
  if ([502, 503, 504].includes(error?.response?.status)) {
    return 'The TrainReservation backend is unavailable. Check that the backend is running and try again.'
  }
  if (!error?.response) return 'Cannot reach the TrainReservation server. Check that the backend is running.'
  return 'The request could not be completed. Please review the details and try again.'
}

export default api
