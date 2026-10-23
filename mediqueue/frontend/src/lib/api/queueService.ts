import { api } from '@/lib/api/client';
import {
  DoctorQueueSummaryResponse,
  QueueEntryResponse,
  QueueEventResponse,
  QueueSessionResponse,
  WaitTimePredictionResponse,
} from '@/types/queue';

export const queueService = {
  async checkInAppointment(appointmentId: number): Promise<QueueEntryResponse> {
    const res = await api.post<QueueEntryResponse>('/api/v1/queue/check-in', {
      appointmentId,
    });
    return res.data;
  },

  async getMyActiveQueueStatus(): Promise<QueueEntryResponse> {
    const res = await api.get<QueueEntryResponse>('/api/v1/queue/my-status');
    return res.data;
  },

  async getMyWaitTimeEstimate(): Promise<WaitTimePredictionResponse> {
    const res = await api.get<WaitTimePredictionResponse>('/api/v1/queue/my-status/wait-estimate');
    return res.data;
  },

  async getDoctorQueueToday(doctorId: number): Promise<DoctorQueueSummaryResponse> {
    const res = await api.get<DoctorQueueSummaryResponse>(`/api/v1/doctors/${doctorId}/queue/today`);
    return res.data;
  },

  async callNext(doctorId?: number): Promise<QueueEntryResponse> {
    const res = await api.post<QueueEntryResponse>(
      '/api/v1/doctor/queue/call-next',
      null,
      doctorId ? { params: { doctorId } } : undefined
    );
    return res.data;
  },

  async callEntry(queueEntryId: number): Promise<QueueEntryResponse> {
    const res = await api.post<QueueEntryResponse>(`/api/v1/queue/${queueEntryId}/call`);
    return res.data;
  },

  async recallEntry(queueEntryId: number): Promise<QueueEntryResponse> {
    const res = await api.post<QueueEntryResponse>(`/api/v1/queue/${queueEntryId}/recall`);
    return res.data;
  },

  async skipEntry(queueEntryId: number, reason?: string): Promise<QueueEntryResponse> {
    const res = await api.post<QueueEntryResponse>(`/api/v1/queue/${queueEntryId}/skip`, {
      reason,
    });
    return res.data;
  },

  async requeueEntry(queueEntryId: number): Promise<QueueEntryResponse> {
    const res = await api.post<QueueEntryResponse>(`/api/v1/queue/${queueEntryId}/requeue`);
    return res.data;
  },

  async startConsultation(queueEntryId: number): Promise<QueueEntryResponse> {
    const res = await api.post<QueueEntryResponse>(`/api/v1/queue/${queueEntryId}/start`);
    return res.data;
  },

  async completeConsultation(queueEntryId: number): Promise<QueueEntryResponse> {
    const res = await api.post<QueueEntryResponse>(`/api/v1/queue/${queueEntryId}/complete`);
    return res.data;
  },

  async pauseQueue(reason?: string, doctorId?: number): Promise<QueueSessionResponse> {
    const res = await api.post<QueueSessionResponse>(
      '/api/v1/doctor/queue/pause',
      reason ? { reason } : undefined,
      doctorId ? { params: { doctorId } } : undefined
    );
    return res.data;
  },

  async resumeQueue(doctorId?: number): Promise<QueueSessionResponse> {
    const res = await api.post<QueueSessionResponse>(
      '/api/v1/doctor/queue/resume',
      null,
      doctorId ? { params: { doctorId } } : undefined
    );
    return res.data;
  },

  async getQueueEvents(queueEntryId: number): Promise<QueueEventResponse[]> {
    const res = await api.get<QueueEventResponse[]>(`/api/v1/queue/${queueEntryId}/events`);
    return res.data;
  },
};
