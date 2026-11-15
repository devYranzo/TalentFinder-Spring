export interface IndexingStatus {
    is_indexing: boolean
    processed: number
    total: number
    progress_percent: number
    error: string | null
    has_data: boolean
    vectors_count: number
    documents_count: number
    total_pdfs: number
}

export interface IndexingActionResult {
    status: string // 'started' | 'already_running' | 'error'
    message?: string
}