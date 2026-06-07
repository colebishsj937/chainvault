export interface PageRequest {
  page: number
  size: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}
