import { ref } from 'vue'
import { defineStore } from 'pinia'

import type { IndexingStatus } from '@/types/indexing'

function emptyStatus(): IndexingStatus {
    return {
        is_indexing: false,
        processed: 0,
        total: 0,
        progress_percent: 0,
        error: null,
        has_data: false,
        vectors_count: 0,
        documents_count: 0,
        total_pdfs: 0,
    }
}

// Solo estado. La lógica y el sondeo viven en useMotorStatus().
export const useIndexingStore = defineStore('indexing', () => {
    const status = ref<IndexingStatus>(emptyStatus())
    const loadingIngest = ref(false)
    const error = ref('')

    // Se incrementa en reset(): invalida peticiones y sondeos en vuelo (p. ej. al cerrar sesión).
    const requestId = ref(0)

    function reset() {
        requestId.value++
        status.value = emptyStatus()
        loadingIngest.value = false
        error.value = ''
    }

    return { status, loadingIngest, error, requestId, reset }
})