export interface ApiError {
  code: string
  message: string
  path: string
  traceId: string
  timestamp: string
  status?: number
}

export interface PageResponse<T> {
  content: T[]
  totalElements?: number
  totalPages?: number
  size?: number
  number?: number
  first?: boolean
  last?: boolean
  empty?: boolean
  page?: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}

export interface ItemPageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
  totalPages: number
}

export interface PageQuery {
  page: number
  size: number
  sort?: string
}
