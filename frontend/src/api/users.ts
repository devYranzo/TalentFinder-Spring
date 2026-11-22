
import api from '@/api/client'
import type { Role } from '@/types'

export interface User {
    id: number
    firstName: string
    lastName: string
    email: string
    role: Role
    active: boolean
}

export interface Member {
    id: number
    firstName: string
    lastName: string
}

export interface UserForm {
    id: number | null
    firstName: string
    lastName: string
    email: string
    password: string
    role: Role
    active: boolean
}

export type CreateUserPayload = Pick<
    UserForm,
    'firstName' | 'lastName' | 'email' | 'password' | 'role'
>

export type EditUserPayload = Partial<
    Pick<UserForm, 'firstName' | 'lastName' | 'role' | 'active'>
>

export const usersApi = {
    async getAllUsers(): Promise<User[]> {
        const { data } = await api.get<User[]>('/users')
        return data
    },

    async getOrgMembers(): Promise<Member[]> {
        const { data } = await api.get<Member[]>('/users/members')
        return data
    },

    async createUser(payload: CreateUserPayload): Promise<User> {
        const { data } = await api.post<User>('/users', payload)
        return data
    },

    async editUser(
        id: number,
        payload: EditUserPayload,
    ): Promise<User> {
        const { data } = await api.patch<User>(`/users/${id}`, payload)
        return data
    },

    async deleteUser(id: number): Promise<void> {
        await api.delete(`/users/${id}`)
    },
}

export default usersApi
