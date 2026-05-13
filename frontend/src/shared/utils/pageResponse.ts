import type { PageResponse } from '@/shared/types/api'

export function getTotalElements(page: PageResponse<unknown> | undefined): number {
  return page?.totalElements ?? page?.page?.totalElements ?? 0
}
