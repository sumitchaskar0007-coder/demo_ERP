export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ApiErrorResponse {
  success: false;
  message: string;
  errors?: Record<string, string> | string[] | null;
  timestamp?: string;
  path?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
