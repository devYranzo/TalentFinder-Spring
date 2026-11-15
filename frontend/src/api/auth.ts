import client from '@/api/client'
import type { Profile } from '@/types'

export const authApi = {
    async login(email: string, password: string) {
        await client.post('/auth/login', { email, password })
    },

    async logout() {
        await client.post('/auth/logout')
    },

    async me(): Promise<Profile> {
        const { data } = await client.get<Profile>('/auth/me')
        return data
    },
}