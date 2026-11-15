import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin } from '@tanstack/vue-query'

import App from './App.vue'
import router from './router'
import { queryClient } from './api/queryClient'
import { setUnauthorizedHandler } from './api/client'
import { useAuthStore } from './stores/auth'

import './assets/styles/main.scss'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import 'bootstrap-icons/font/bootstrap-icons.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(VueQueryPlugin, { queryClient })

// Sesión caducada en cualquier petición -> limpiar estado y volver al login
setUnauthorizedHandler(() => {
    useAuthStore().clear()

    const current = router.currentRoute.value
    if (current.name !== 'login') {
        router.replace({ name: 'login', query: { redirect: current.fullPath } })
    }
})

app.mount('#app')