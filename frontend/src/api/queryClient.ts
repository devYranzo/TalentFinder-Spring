import axios from 'axios'
import { QueryClient } from '@tanstack/vue-query'

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 30_000,
            refetchOnWindowFocus: false,
            // No reintentar errores 4xx (permisos, no encontrado, validación...)
            retry: (failureCount, error) => {
                if (axios.isAxiosError(error) && error.response && error.response.status < 500) {
                    return false
                }
                return failureCount < 2
            },
        },
    },
})