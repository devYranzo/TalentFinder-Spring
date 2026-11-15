import {computed, ref} from 'vue'
import {defineStore} from 'pinia'

import {authApi} from '@/api/auth'
import {queryClient} from '@/api/queryClient'
import type {Profile, Role} from '@/types'
import {useSearchStore} from "@/stores/search.ts";

export const useAuthStore = defineStore('auth', () => {
    const profile = ref<Profile | null>(null)
    // true cuando ya hemos preguntado al backend si hay sesión (con o sin éxito)
    const loaded = ref(false)

    const isAuthenticated = computed(() => profile.value !== null)
    const role = computed<Role | null>(() => profile.value?.role ?? null)
    const fullName = computed(() => {
        if (!profile.value) return ''
        return `${profile.value.firstName ?? ''} ${profile.value.lastName ?? ''}`.trim()
    })

    function hasRole(allowed: readonly Role[]): boolean {
        return role.value !== null && allowed.includes(role.value)
    }

    /** La cookie es httpOnly: esta llamada es la única forma de saber si hay sesión. */
    async function fetchMe() {
        try {
            profile.value = await authApi.me()
        } catch {
            profile.value = null
        } finally {
            loaded.value = true
        }
    }

    async function login(email: string, password: string) {
        await authApi.login(email, password)
        await fetchMe()
        if (!profile.value) {
            throw new Error('No se pudo cargar el perfil del usuario.')
        }
    }

    /** Limpia el estado local y la caché. */
    function clear() {
        profile.value = null
        loaded.value = true
        queryClient.clear()
        useSearchStore().reset()
    }

    async function logout() {
        try {
            await authApi.logout()
        } finally {
            clear()
        }
    }

    return { profile, loaded, isAuthenticated, role, fullName, hasRole, fetchMe, login, logout, clear }
})