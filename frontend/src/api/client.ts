import axios from 'axios'

const client = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? '/api',
    withCredentials: true,
    timeout: 120_000,
})

let onUnauthorized: (() => void) | null = null

export function setUnauthorizedHandler(handler: () => void) {
    onUnauthorized = handler
}

// Un 401 en estas rutas es esperado y lo gestiona quien las llama.
const SILENT_401 = ['/auth/login', '/auth/me']

client.interceptors.response.use(
    (response) => response,
    (error) => {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
            const url = error.config?.url ?? ''
            if (!SILENT_401.some((path) => url.startsWith(path))) {
                onUnauthorized?.()
            }
        }
        return Promise.reject(error)
    },
)

export default client