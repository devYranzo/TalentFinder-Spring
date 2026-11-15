import { computed, toValue, type MaybeRefOrGetter } from 'vue'
import { storeToRefs } from 'pinia'

import { getErrorMessage } from '@/api/errors'
import { searchApi } from '@/api/search'
import { useSearchStore } from '@/stores/search'

const MIN_QUERY_LENGTH = 3
const MAX_QUERY_LENGTH = 500

let copiadoTimer: ReturnType<typeof setTimeout> | undefined

/** Quita el marcador del botón y el markdown para dejar texto plano. */
function limpiarMarkdown(texto: string): string {
    return texto
        .replace(/\[BOTON_CV:[^\]]*\]/g, '')
        .replace(/^\s*---+\s*$/gm, '')
        .replace(/^#{1,6}\s+/gm, '')
        .replace(/^\s*[-*]\s+/gm, '• ')
        .replace(/\*\*(.+?)\*\*/g, '$1')
        .replace(/\*(.+?)\*/g, '$1')
        .replace(/\n{3,}/g, '\n\n')
        .trim()
}

/** isReady: opcional (ref, getter o booleano). Por ejemplo, "hay CVs indexados". */
export function useSearch(isReady: MaybeRefOrGetter<boolean> = true) {
    const store = useSearchStore()
    const { query, respuesta, sources, loading, error, copiado } = storeToRefs(store)

    const canSearch = computed(
        () => toValue(isReady) && !loading.value && query.value.trim().length >= MIN_QUERY_LENGTH,
    )

    async function buscar() {
        if (!canSearch.value) return

        const id = ++store.requestId

        loading.value = true
        respuesta.value = ''
        sources.value = []
        error.value = ''

        try {
            const res = await searchApi.query(query.value.trim().slice(0, MAX_QUERY_LENGTH))
            if (id !== store.requestId) return // reset() (p. ej. logout) mientras esperábamos
            respuesta.value = res.answer
            sources.value = res.sources
        } catch (e) {
            if (id !== store.requestId) return
            error.value = getErrorMessage(e)
        } finally {
            if (id === store.requestId) loading.value = false
        }
    }

    async function copiarAlPortapapeles() {
        if (!respuesta.value) return

        try {
            await navigator.clipboard.writeText(limpiarMarkdown(respuesta.value))
            copiado.value = true
            clearTimeout(copiadoTimer)
            copiadoTimer = setTimeout(() => {
                copiado.value = false
            }, 2000)
        } catch (e) {
            console.error('Error al copiar:', e)
        }
    }

    return {
        query,
        respuesta,
        sources,
        loading,
        error,
        copiado,
        canSearch,
        buscar,
        copiarAlPortapapeles,
        reset: store.reset,
    }
}