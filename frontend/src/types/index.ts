export type Role = 'admin' | 'recruiter' | 'viewer'

export interface Profile {
    profile_id: number | null
    first_name: string | null
    last_name: string | null
    role: Role
    email: string
    is_active: boolean
}