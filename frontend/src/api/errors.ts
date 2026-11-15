import axios from 'axios'

export function getErrorMessage(error: unknown, fallback = 'Ha ocurrido un error inesperado.'): string {
    if (axios.isAxiosError(error)) {
        if (!error.response) return 'No se pudo conectar con el servidor.'

        const detail = error.response.data?.detail
        if (typeof detail === 'string') return detail
        if (Array.isArray(detail) && detail.length > 0) {
            return detail.map((d: { msg?: string }) => d.msg ?? 'Dato no válido').join('. ')
        }
        return fallback
    }

    if (error instanceof Error) return error.message
    return fallback
}