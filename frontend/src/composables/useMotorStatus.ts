import { computed, getCurrentScope, onScopeDispose } from 'vue'
import { storeToRefs } from 'pinia'

import { getErrorMessage } from '@/api/errors'
import { indexingApi } from '@/api/indexing'
import { useIndexingStore } from '@/stores/indexing'
import type { IndexingActionResult } from '@/types/indexing'

const POLL_MS = 2500
// Tras lanzar la indexación, el backend tarda un instante en marcar is_indexing=true.
// Esperamos hasta ~15 s a verlo antes de dar el sondeo por terminado.
const MAX_IDLE_POLLS = 6

// Un único sondeo para toda la app, aunque haya varias vistas usando el composable
let timer: ReturnType<typeof setTimeout> | undefined
let polling = false
let pollSession = -1
let consumers = 0

function clearTimer() {
    clearTimeout(timer)
    timer = undefined
    polling = false
}

export function useMotorStatus() {
    const store = useIndexingStore()
    const { status, loadingIngest, error } = storeToRefs(store)

    const isReady = computed(() => !status.value.is_indexing && status.value.has_data)

    async function fetchStatus() {
        const id = store.requestId
        try {
            const data = await indexingApi.status()
            if (id !== store.requestId) return null // reset() mientras esperábamos
            status.value = data
            error.value = ''
            return data
        } catch (e) {
            if (id === store.requestId) error.value = getErrorMessage(e)
            return null
        }
    }

    function startPolling(waitForStart = false) {
        const id = store.requestId
        if (polling && pollSession === id) return

        clearTimer() // sondeo de una sesión anterior
        polling = true
        pollSession = id

        let idlePolls = 0
        let seenIndexing = !waitForStart

        const tick = async () => {
            if (id !== store.requestId) return clearTimer()

            const data = await fetchStatus()
            if (id !== store.requestId) return clearTimer()

            if (!data) {
                // Error de red o 401: se muestra en `error` y paramos
                loadingIngest.value = false
                return clearTimer()
            }

            if (data.is_indexing) {
                seenIndexing = true
            } else if (seenIndexing || ++idlePolls >= MAX_IDLE_POLLS) {
                loadingIngest.value = false
                return clearTimer()
            }

            timer = setTimeout(tick, POLL_MS)
        }

        timer = setTimeout(tick, POLL_MS)
    }

    /** Cargar al entrar en la vista. Si ya hay una indexación en curso, engancha el sondeo. */
    async function init() {
        const data = await fetchStatus()
        if (data?.is_indexing) startPolling()
    }

    async function lanzar(action: () => Promise<IndexingActionResult>) {
        if (loadingIngest.value || status.value.is_indexing) return

        const id = store.requestId
        loadingIngest.value = true
        error.value = ''

        try {
            const res = await action()
            if (id !== store.requestId) return

            if (res.status === 'error') {
                error.value = res.message ?? 'No se pudo iniciar la indexación.'
                loadingIngest.value = false
                return
            }
            // 'started' o 'already_running'
            startPolling(true)
        } catch (e) {
            if (id !== store.requestId) return
            error.value = getErrorMessage(e)
            loadingIngest.value = false
        }
    }

    const encenderMotor = () => lanzar(indexingApi.start)
    const reindexar = () => lanzar(indexingApi.reindex)

    // Al salir de la última vista que lo usa, paramos el sondeo.
    // Al volver, init() lo reengancha si todavía se está indexando.
    if (getCurrentScope()) {
        consumers++
        onScopeDispose(() => {
            consumers = Math.max(0, consumers - 1)
            if (consumers === 0) clearTimer()
        })
    }

    return {
        motorStatus: status,
        loadingIngest,
        error,
        isReady,
        init,
        encenderMotor,
        reindexar,
        fetchStatus,
    }
}