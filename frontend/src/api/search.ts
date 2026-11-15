import client from '@/api/client'
import type { SearchResult } from '@/types/search'

export const searchApi = {
    async query(question: string): Promise<SearchResult> {
        const { data } = await client.post<SearchResult>('/query', { question })
        return data
    },
}