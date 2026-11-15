import client from '@/api/client'
import type { IndexingActionResult, IndexingStatus } from '@/types/indexing'

export const indexingApi = {
    async status(): Promise<IndexingStatus> {
        const { data } = await client.get<IndexingStatus>('/index/status')
        return data
    },

    async start(): Promise<IndexingActionResult> {
        const { data } = await client.post<IndexingActionResult>('/index/start')
        return data
    },

    async reindex(): Promise<IndexingActionResult> {
        const { data } = await client.post<IndexingActionResult>('/index/reindex')
        return data
    },
}