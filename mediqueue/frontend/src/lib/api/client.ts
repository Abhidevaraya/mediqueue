import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { tokenStorage } from '@/lib/auth/tokenStorage';
import { ApiErrorResponse, ApiResponse } from '@/types/api';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export class AppApiError extends Error {
  public status?: number;
  public errorCode?: string;
  public fieldErrors?: Record<string, string>;
  public details?: string[];
  public path?: string;

  constructor(
    message: string,
    status?: number,
    errorCode?: string,
    fieldErrors?: Record<string, string>,
    details?: string[],
    path?: string
  ) {
    super(message);
    this.name = 'AppApiError';
    this.status = status;
    this.errorCode = errorCode;
    this.fieldErrors = fieldErrors;
    this.details = details;
    this.path = path;
  }
}

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

// Request Interceptor: Attach Bearer Token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenStorage.getToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Normalize Errors & Handle 401 Session Invalidation
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    if (!error.response) {
      // Network failure or timeout
      const isTimeout = error.code === 'ECONNABORTED';
      return Promise.reject(
        new AppApiError(
          isTimeout
            ? 'Connection timed out. Please check your network and try again.'
            : 'Unable to connect to MediQueue backend server.',
          0,
          'NETWORK_ERROR'
        )
      );
    }

    const { status, data } = error.response;

    if (status === 401) {
      // Clear token to invalidate session if unauthorized
      tokenStorage.clearToken();
      window.dispatchEvent(new CustomEvent('mediqueue:auth_expired'));
    }

    const errorCode = data?.errorCode;
    const details = Array.isArray(data?.details) ? data.details : undefined;
    const path = data?.path;

    // Safely extract field names and messages from details: string[] formatted as "field: message"
    const fieldErrors: Record<string, string> = { ...(data?.errors || {}) };
    if (details) {
      details.forEach((detail) => {
        const colonIdx = detail.indexOf(':');
        if (colonIdx > 0) {
          const field = detail.slice(0, colonIdx).trim();
          const msg = detail.slice(colonIdx + 1).trim();
          if (field && msg && !fieldErrors[field]) {
            fieldErrors[field] = msg;
          }
        }
      });
    }

    let message = data?.message;
    if (!message) {
      if (status === 400) message = 'Invalid request parameters';
      else if (status === 401) message = 'Authentication required or session expired';
      else if (status === 403) message = 'Access forbidden: insufficient permissions';
      else if (status === 404) message = 'Requested resource not found';
      else if (status === 409) message = 'Conflict: operation cannot be performed in current state';
      else if (status === 429) message = 'Too many requests. Please slow down and try again.';
      else if (status >= 500) message = 'An unexpected server error occurred. Please try again later.';
      else message = 'An error occurred processing your request';
    } else if (message === 'Validation failed' && details && details.length > 0) {
      message = `Validation failed: ${details.join(', ')}`;
    }

    return Promise.reject(
      new AppApiError(
        message,
        status,
        errorCode,
        Object.keys(fieldErrors).length > 0 ? fieldErrors : undefined,
        details,
        path
      )
    );
  }
);

// Typed API Helper Functions
export const api = {
  async get<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await apiClient.get<ApiResponse<T>>(url, { params, ...config });
    return res.data;
  },

  async post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await apiClient.post<ApiResponse<T>>(url, body, config);
    return res.data;
  },

  async put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await apiClient.put<ApiResponse<T>>(url, body, config);
    return res.data;
  },

  async patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await apiClient.patch<ApiResponse<T>>(url, body, config);
    return res.data;
  },

  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    const res = await apiClient.delete<ApiResponse<T>>(url, config);
    return res.data;
  },
};
