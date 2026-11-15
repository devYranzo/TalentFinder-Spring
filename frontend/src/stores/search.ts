import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useSearchStore = defineStore('search', () => {
    const query = ref('')
    const respuesta = ref('')
    const sources = ref<string[]>([])
    const loading = ref(false)
    const error = ref('')
    const copiado = ref(false)

    const requestId = ref(0)

    function reset() {
        requestId.value++
        query.value = ''
        respuesta.value = ''
        sources.value = []
        loading.value = false
        error.value = ''
        copiado.value = false
    }

    return { query, respuesta, sources, loading, error, copiado, requestId, reset }
})