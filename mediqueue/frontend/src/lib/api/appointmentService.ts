import { api } from '@/lib/api/client';
import { PageResponse } from '@/types/api';
import {
  AppointmentBookingRequest,
  AppointmentResponse,
  AppointmentStatus,
} from '@/types/appointment';

export const appointmentService = {
  async bookAppointment(request: AppointmentBookingRequest): Promise<AppointmentResponse> {
    const res = await api.post<AppointmentResponse>('/api/v1/appointments', request);
    return res.data;
  },

  async getMyAppointments(
    status?: AppointmentStatus,
    page = 0,
    size = 20
  ): Promise<PageResponse<AppointmentResponse>> {
    const res = await api.get<PageResponse<AppointmentResponse>>('/api/v1/appointments/my', {
      status,
      page,
      size,
    });
    return res.data;
  },

  async getAppointmentById(id: number): Promise<AppointmentResponse> {
    const res = await api.get<AppointmentResponse>(`/api/v1/appointments/${id}`);
    return res.data;
  },

  async cancelAppointment(id: number, reason?: string): Promise<AppointmentResponse> {
    const res = await api.patch<AppointmentResponse>(`/api/v1/appointments/${id}/cancel`, {
      reason,
    });
    return res.data;
  },

  async rescheduleAppointment(
    id: number,
    newStartDateTime: string
  ): Promise<AppointmentResponse> {
    const res = await api.patch<AppointmentResponse>(`/api/v1/appointments/${id}/reschedule`, {
      newStartDateTime,
    });
    return res.data;
  },

  async getDoctorTodayAppointments(): Promise<AppointmentResponse[]> {
    const res = await api.get<AppointmentResponse[]>('/api/v1/doctor/appointments/today');
    return res.data;
  },
};
