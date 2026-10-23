import { api } from '@/lib/api/client';
import { PageResponse } from '@/types/api';
import {
  DepartmentResponse,
  DoctorPublicResponse,
  DoctorScheduleResponse,
  ScheduleExceptionResponse,
  SlotResponse,
} from '@/types/doctor';

export const doctorService = {
  async getDepartments(page = 0, size = 50): Promise<PageResponse<DepartmentResponse>> {
    const res = await api.get<PageResponse<DepartmentResponse>>('/api/v1/departments', {
      page,
      size,
    });
    return res.data;
  },

  async getDoctors(params?: {
    departmentId?: number;
    specialty?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<PageResponse<DoctorPublicResponse>> {
    const res = await api.get<PageResponse<DoctorPublicResponse>>('/api/v1/doctors', params);
    return res.data;
  },

  async getDoctorById(id: number): Promise<DoctorPublicResponse> {
    const res = await api.get<DoctorPublicResponse>(`/api/v1/doctors/${id}`);
    return res.data;
  },

  async getAvailableSlots(doctorId: number, date: string): Promise<SlotResponse[]> {
    const res = await api.get<SlotResponse[]>(`/api/v1/doctors/${doctorId}/slots`, {
      date,
    });
    return res.data;
  },

  async getDoctorSchedules(doctorId: number): Promise<DoctorScheduleResponse[]> {
    const res = await api.get<DoctorScheduleResponse[]>(`/api/v1/doctors/${doctorId}/schedules`);
    return res.data;
  },

  async createSchedule(payload: {
    dayOfWeek: string;
    startTime: string;
    endTime: string;
    slotDurationMinutes: number;
    breakStartTime?: string;
    breakEndTime?: string;
  }): Promise<DoctorScheduleResponse> {
    const res = await api.post<DoctorScheduleResponse>('/api/v1/doctor/schedules', payload);
    return res.data;
  },

  async updateSchedule(
    id: number,
    payload: {
      startTime: string;
      endTime: string;
      slotDurationMinutes: number;
      breakStartTime?: string;
      breakEndTime?: string;
      isActive: boolean;
    }
  ): Promise<DoctorScheduleResponse> {
    const res = await api.put<DoctorScheduleResponse>(`/api/v1/doctor/schedules/${id}`, payload);
    return res.data;
  },

  async createException(payload: {
    exceptionDate: string;
    isAvailable: boolean;
    reason?: string;
    customStartTime?: string;
    customEndTime?: string;
  }): Promise<ScheduleExceptionResponse> {
    const res = await api.post<ScheduleExceptionResponse>('/api/v1/doctor/schedules/exceptions', payload);
    return res.data;
  },

  async getExceptions(): Promise<ScheduleExceptionResponse[]> {
    const res = await api.get<ScheduleExceptionResponse[]>('/api/v1/doctor/schedules/exceptions');
    return res.data;
  },
};
