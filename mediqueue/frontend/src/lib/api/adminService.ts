import { api, apiClient } from '@/lib/api/client';
import { PageResponse } from '@/types/api';
import { UserRole } from '@/types/auth';
import { DepartmentResponse, DoctorScheduleResponse } from '@/types/doctor';
import {
  AccountStatus,
  AdminQueueSummaryResponse,
  AppointmentAnalyticsResponse,
  ClinicalAuditEvent,
  DepartmentCreateRequest,
  DepartmentUpdateRequest,
  DepartmentWorkloadResponse,
  DoctorAdminResponse,
  DoctorCreateRequest,
  DoctorUpdateRequest,
  DoctorVerificationStatus,
  DoctorWorkloadResponse,
  HospitalOverviewResponse,
  QueueSessionResponse,
  UserResponse,
  WaitTimeAnalyticsResponse,
} from '@/types/admin';

export const adminService = {
  // --- Analytics ---
  async getOverview(): Promise<HospitalOverviewResponse> {
    const res = await api.get<HospitalOverviewResponse>('/api/v1/admin/analytics/overview');
    return res.data;
  },

  async getAppointmentAnalytics(params?: {
    from?: string;
    to?: string;
  }): Promise<AppointmentAnalyticsResponse> {
    const res = await api.get<AppointmentAnalyticsResponse>('/api/v1/admin/analytics/appointments', params);
    return res.data;
  },

  async getWaitTimeAnalytics(params?: {
    from?: string;
    to?: string;
  }): Promise<WaitTimeAnalyticsResponse> {
    const res = await api.get<WaitTimeAnalyticsResponse>('/api/v1/admin/analytics/wait-times', params);
    return res.data;
  },

  async getDoctorWorkload(params?: {
    from?: string;
    to?: string;
  }): Promise<DoctorWorkloadResponse[]> {
    const res = await api.get<DoctorWorkloadResponse[]>('/api/v1/admin/analytics/doctors', params);
    return res.data;
  },

  async getDepartmentWorkload(params?: {
    from?: string;
    to?: string;
  }): Promise<DepartmentWorkloadResponse[]> {
    const res = await api.get<DepartmentWorkloadResponse[]>('/api/v1/admin/analytics/departments', params);
    return res.data;
  },

  async exportAppointmentsCsv(params?: { from?: string; to?: string }): Promise<Blob> {
    const res = await apiClient.get('/api/v1/admin/analytics/appointments/export', {
      params,
      responseType: 'blob',
    });
    return res.data;
  },

  async exportWaitTimesCsv(params?: { from?: string; to?: string }): Promise<Blob> {
    const res = await apiClient.get('/api/v1/admin/analytics/wait-times/export', {
      params,
      responseType: 'blob',
    });
    return res.data;
  },

  // --- Users ---
  async getUsers(params?: {
    role?: UserRole;
    status?: AccountStatus;
    search?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<PageResponse<UserResponse>> {
    const res = await api.get<PageResponse<UserResponse>>('/api/v1/admin/users', params);
    return res.data;
  },

  async getUserById(id: number): Promise<UserResponse> {
    const res = await api.get<UserResponse>(`/api/v1/admin/users/${id}`);
    return res.data;
  },

  async updateUserStatus(id: number, status: AccountStatus): Promise<UserResponse> {
    const res = await api.patch<UserResponse>(`/api/v1/admin/users/${id}/status`, { status });
    return res.data;
  },

  async updateUserRole(id: number, role: UserRole): Promise<UserResponse> {
    const res = await api.patch<UserResponse>(`/api/v1/admin/users/${id}/role`, { role });
    return res.data;
  },

  // --- Doctors ---
  async getDoctors(params?: {
    verificationStatus?: DoctorVerificationStatus;
    departmentId?: number;
    page?: number;
    size?: number;
  }): Promise<PageResponse<DoctorAdminResponse>> {
    const res = await api.get<PageResponse<DoctorAdminResponse>>('/api/v1/admin/doctors', params);
    return res.data;
  },

  async getDoctorById(id: number): Promise<DoctorAdminResponse> {
    const res = await api.get<DoctorAdminResponse>(`/api/v1/admin/doctors/${id}`);
    return res.data;
  },

  async createDoctor(data: DoctorCreateRequest): Promise<DoctorAdminResponse> {
    const res = await api.post<DoctorAdminResponse>('/api/v1/admin/doctors', data);
    return res.data;
  },

  async updateDoctor(id: number, data: DoctorUpdateRequest): Promise<DoctorAdminResponse> {
    const res = await api.put<DoctorAdminResponse>(`/api/v1/admin/doctors/${id}`, data);
    return res.data;
  },

  async updateDoctorVerification(
    id: number,
    verificationStatus: DoctorVerificationStatus,
    _reason?: string
  ): Promise<DoctorAdminResponse> {
    const res = await api.patch<DoctorAdminResponse>(`/api/v1/admin/doctors/${id}/verification`, {
      verificationStatus,
    });
    return res.data;
  },

  async updateDoctorStatus(
    id: number,
    status: AccountStatus,
    _reason?: string
  ): Promise<DoctorAdminResponse> {
    const res = await api.patch<DoctorAdminResponse>(`/api/v1/admin/doctors/${id}/status`, {
      accountStatus: status,
    });
    return res.data;
  },

  // --- Departments ---
  async getDepartments(): Promise<DepartmentResponse[]> {
    const res = await api.get<PageResponse<DepartmentResponse> | DepartmentResponse[]>('/api/v1/admin/departments', {
      size: 100,
    });
    const payload = res.data as unknown;
    if (
      payload &&
      typeof payload === 'object' &&
      'content' in payload &&
      Array.isArray((payload as { content: unknown }).content)
    ) {
      return (payload as { content: DepartmentResponse[] }).content;
    }
    return Array.isArray(payload) ? (payload as DepartmentResponse[]) : [];
  },

  async getDepartmentById(id: number): Promise<DepartmentResponse> {
    const res = await api.get<DepartmentResponse>(`/api/v1/admin/departments/${id}`);
    return res.data;
  },

  async createDepartment(data: DepartmentCreateRequest): Promise<DepartmentResponse> {
    const res = await api.post<DepartmentResponse>('/api/v1/admin/departments', data);
    return res.data;
  },

  async updateDepartment(id: number, data: DepartmentUpdateRequest): Promise<DepartmentResponse> {
    const res = await api.put<DepartmentResponse>(`/api/v1/admin/departments/${id}`, data);
    return res.data;
  },

  async updateDepartmentStatus(id: number, active: boolean): Promise<DepartmentResponse> {
    const res = await api.patch<DepartmentResponse>(
      `/api/v1/admin/departments/${id}/status`,
      { active }
    );
    return res.data;
  },

  // --- Schedules ---
  async getSchedules(params?: {
    doctorId?: number;
    departmentId?: number;
    dayOfWeek?: string;
  }): Promise<DoctorScheduleResponse[]> {
    const res = await api.get<DoctorScheduleResponse[]>('/api/v1/admin/schedules', params);
    return res.data;
  },

  // --- Queues ---
  async getTodayQueues(): Promise<AdminQueueSummaryResponse[]> {
    const res = await api.get<AdminQueueSummaryResponse[]>('/api/v1/admin/queues');
    return res.data;
  },

  async pauseQueue(sessionId: number): Promise<QueueSessionResponse> {
    const res = await api.post<QueueSessionResponse>(`/api/v1/admin/queues/${sessionId}/pause`);
    return res.data;
  },

  async resumeQueue(sessionId: number): Promise<QueueSessionResponse> {
    const res = await api.post<QueueSessionResponse>(`/api/v1/admin/queues/${sessionId}/resume`);
    return res.data;
  },

  // --- Audit Logs ---
  async getAuditLogs(params?: {
    actorId?: number;
    actorUserId?: number;
    actorRole?: string;
    action?: string;
    resourceType?: string;
    resourceId?: number;
    outcome?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<ClinicalAuditEvent>> {
    const queryParams: Record<string, unknown> = { ...params };
    if (params?.actorUserId && !params?.actorId) {
      queryParams.actorId = params.actorUserId;
    }
    const res = await api.get<PageResponse<ClinicalAuditEvent>>('/api/v1/admin/audit-logs', queryParams);
    return res.data;
  },

  async exportAuditLogsCsv(params?: {
    actorId?: number;
    actorUserId?: number;
    actorRole?: string;
    action?: string;
    resourceType?: string;
    resourceId?: number;
    outcome?: string;
    from?: string;
    to?: string;
  }): Promise<Blob> {
    const queryParams: Record<string, unknown> = { ...params };
    if (params?.actorUserId && !params?.actorId) {
      queryParams.actorId = params.actorUserId;
    }
    const res = await apiClient.get('/api/v1/admin/audit-logs/export', {
      params: queryParams,
      responseType: 'blob',
    });
    return res.data;
  },
};
