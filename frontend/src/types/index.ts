export type Role = 'ADMIN' | 'RECRUITER';

export interface Profile {
    id: number | null
    firstName: string | null
    lastName: string | null
    email: string
    role: Role
    active: boolean
}