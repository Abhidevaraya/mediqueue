import { api } from '@/lib/api/client';
import { AuthResponse, DoctorRegisterPayload, LoginCredentials, RegisterPayload, UserSummary } from '@/types/auth';

export const authService = {
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const res = await api.post<AuthResponse>('/api/v1/auth/login', credentials);
    return res.data;
  },

  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const res = await api.post<AuthResponse>('/api/v1/auth/register', payload);
    return res.data;
  },

  async registerDoctor(payload: DoctorRegisterPayload): Promise<AuthResponse> {
    const res = await api.post<AuthResponse>('/api/v1/auth/register-doctor', payload);
    return res.data;
  },

  async getCurrentUser(): Promise<UserSummary> {
    const res = await api.get<UserSummary>('/api/v1/auth/me');
    return res.data;
  },

  async logout(): Promise<void> {
    try {
      await api.post<void>('/api/v1/auth/logout');
    } catch {
      // Best-effort server notification; local state is cleared regardless
    }
  },
};
