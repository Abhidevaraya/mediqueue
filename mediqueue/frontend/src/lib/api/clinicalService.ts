import { api, apiClient } from '@/lib/api/client';
import { PageResponse } from '@/types/api';
import {
  ConsultationResponse,
  DocumentCategory,
  FollowUpCreateRequest,
  FollowUpResponse,
  MedicalDocumentResponse,
  NotificationResponse,
  PrescriptionCreateRequest,
  PrescriptionResponse,
  PrescriptionRevisionRequest,
  UnreadCountResponse,
  VisitSummaryCreateRequest,
  VisitSummaryResponse,
  VisitSummaryRevisionRequest,
  VisitSummaryUpdateRequest,
} from '@/types/clinical';

export const clinicalService = {
  // Consultations
  async startConsultation(appointmentId: number): Promise<ConsultationResponse> {
    const res = await api.post<ConsultationResponse>(
      `/api/v1/doctor/consultations/${appointmentId}/start`
    );
    return res.data;
  },

  async completeConsultation(consultationId: number): Promise<ConsultationResponse> {
    const res = await api.post<ConsultationResponse>(
      `/api/v1/doctor/consultations/${consultationId}/complete`
    );
    return res.data;
  },

  async getConsultationById(consultationId: number): Promise<ConsultationResponse> {
    const res = await api.get<ConsultationResponse>(
      `/api/v1/doctor/consultations/${consultationId}`
    );
    return res.data;
  },

  async getDoctorConsultations(page = 0, size = 20): Promise<PageResponse<ConsultationResponse>> {
    const res = await api.get<PageResponse<ConsultationResponse>>(
      '/api/v1/doctor/consultations',
      { page, size }
    );
    return res.data;
  },

  async getPatientConsultations(page = 0, size = 20): Promise<PageResponse<ConsultationResponse>> {
    const res = await api.get<PageResponse<ConsultationResponse>>(
      '/api/v1/consultations/my',
      { page, size }
    );
    return res.data;
  },

  // Visit Summaries
  async createVisitSummaryDraft(request: VisitSummaryCreateRequest): Promise<VisitSummaryResponse> {
    const res = await api.post<VisitSummaryResponse>('/api/v1/medical-records', request);
    return res.data;
  },

  async updateVisitSummaryDraft(
    id: number,
    request: VisitSummaryUpdateRequest
  ): Promise<VisitSummaryResponse> {
    const res = await api.patch<VisitSummaryResponse>(`/api/v1/medical-records/${id}`, request);
    return res.data;
  },

  async approveVisitSummary(id: number): Promise<VisitSummaryResponse> {
    const res = await api.post<VisitSummaryResponse>(`/api/v1/medical-records/${id}/approve`);
    return res.data;
  },

  async reviseVisitSummary(
    id: number,
    request: VisitSummaryRevisionRequest
  ): Promise<VisitSummaryResponse> {
    const res = await api.post<VisitSummaryResponse>(`/api/v1/medical-records/${id}/revise`, request);
    return res.data;
  },

  async getMyMedicalRecords(page = 0, size = 20): Promise<PageResponse<VisitSummaryResponse>> {
    const res = await api.get<PageResponse<VisitSummaryResponse>>(
      '/api/v1/medical-records/my',
      { page, size }
    );
    return res.data;
  },

  async getMedicalRecordById(id: number): Promise<VisitSummaryResponse> {
    const res = await api.get<VisitSummaryResponse>(`/api/v1/medical-records/${id}`);
    return res.data;
  },

  async getDoctorMedicalRecords(page = 0, size = 20): Promise<PageResponse<VisitSummaryResponse>> {
    const res = await api.get<PageResponse<VisitSummaryResponse>>(
      '/api/v1/doctor/medical-records',
      { page, size }
    );
    return res.data;
  },

  // Prescriptions
  async issuePrescription(request: PrescriptionCreateRequest): Promise<PrescriptionResponse> {
    const res = await api.post<PrescriptionResponse>('/api/v1/prescriptions', request);
    return res.data;
  },

  async cancelPrescription(id: number, reason?: string): Promise<PrescriptionResponse> {
    const res = await api.patch<PrescriptionResponse>(
      `/api/v1/prescriptions/${id}/cancel`,
      null,
      reason ? { params: { reason } } : undefined
    );
    return res.data;
  },

  async revisePrescription(
    id: number,
    request: PrescriptionRevisionRequest
  ): Promise<PrescriptionResponse> {
    const res = await api.post<PrescriptionResponse>(`/api/v1/prescriptions/${id}/revise`, request);
    return res.data;
  },

  async getMyPrescriptions(page = 0, size = 20): Promise<PageResponse<PrescriptionResponse>> {
    const res = await api.get<PageResponse<PrescriptionResponse>>(
      '/api/v1/prescriptions/my',
      { page, size }
    );
    return res.data;
  },

  async getPrescriptionById(id: number): Promise<PrescriptionResponse> {
    const res = await api.get<PrescriptionResponse>(`/api/v1/prescriptions/${id}`);
    return res.data;
  },

  async getDoctorPrescriptions(page = 0, size = 20): Promise<PageResponse<PrescriptionResponse>> {
    const res = await api.get<PageResponse<PrescriptionResponse>>(
      '/api/v1/doctor/prescriptions',
      { page, size }
    );
    return res.data;
  },

  // Follow-ups
  async createFollowUp(request: FollowUpCreateRequest): Promise<FollowUpResponse> {
    const res = await api.post<FollowUpResponse>('/api/v1/follow-ups', request);
    return res.data;
  },

  async completeFollowUp(id: number): Promise<FollowUpResponse> {
    const res = await api.patch<FollowUpResponse>(`/api/v1/follow-ups/${id}/complete`);
    return res.data;
  },

  async cancelFollowUp(id: number, reason?: string): Promise<FollowUpResponse> {
    const res = await api.patch<FollowUpResponse>(
      `/api/v1/follow-ups/${id}/cancel`,
      null,
      reason ? { params: { reason } } : undefined
    );
    return res.data;
  },

  async getMyFollowUps(page = 0, size = 20): Promise<PageResponse<FollowUpResponse>> {
    const res = await api.get<PageResponse<FollowUpResponse>>(
      '/api/v1/follow-ups/my',
      { page, size }
    );
    return res.data;
  },

  async getFollowUpById(id: number): Promise<FollowUpResponse> {
    const res = await api.get<FollowUpResponse>(`/api/v1/follow-ups/${id}`);
    return res.data;
  },

  async getDoctorFollowUps(page = 0, size = 20): Promise<PageResponse<FollowUpResponse>> {
    const res = await api.get<PageResponse<FollowUpResponse>>(
      '/api/v1/doctor/follow-ups',
      { page, size }
    );
    return res.data;
  },

  // Medical Documents
  async uploadMedicalDocument(
    patientId: number,
    file: File,
    options?: {
      appointmentId?: number;
      consultationId?: number;
      category?: DocumentCategory;
    }
  ): Promise<MedicalDocumentResponse> {
    const formData = new FormData();
    formData.append('patientId', String(patientId));
    formData.append('file', file);
    if (options?.appointmentId) formData.append('appointmentId', String(options.appointmentId));
    if (options?.consultationId) formData.append('consultationId', String(options.consultationId));
    if (options?.category) formData.append('category', options.category);

    const res = await apiClient.post<{ data: MedicalDocumentResponse } | MedicalDocumentResponse>(
      '/api/v1/medical-documents',
      formData
    );
    const body = res.data;
    return body && typeof body === 'object' && 'data' in body
      ? (body as { data: MedicalDocumentResponse }).data
      : (body as MedicalDocumentResponse);
  },

  async getMyMedicalDocuments(page = 0, size = 20): Promise<PageResponse<MedicalDocumentResponse>> {
    const res = await api.get<PageResponse<MedicalDocumentResponse>>(
      '/api/v1/medical-documents/my',
      { page, size }
    );
    return res.data;
  },

  async downloadMedicalDocument(id: number, filename: string): Promise<void> {
    const res = await apiClient.get<Blob>(`/api/v1/medical-documents/${id}/download`, {
      responseType: 'blob',
    });
    const blob = new Blob([res.data]);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async deleteMedicalDocument(id: number): Promise<void> {
    await api.delete(`/api/v1/medical-documents/${id}`);
  },

  // Notifications
  async getMyNotifications(page = 0, size = 20): Promise<PageResponse<NotificationResponse>> {
    const res = await api.get<PageResponse<NotificationResponse>>(
      '/api/v1/notifications',
      { page, size }
    );
    return res.data;
  },

  async getUnreadNotificationCount(): Promise<UnreadCountResponse> {
    const res = await api.get<UnreadCountResponse>('/api/v1/notifications/unread-count');
    return res.data;
  },

  async markNotificationAsRead(id: number): Promise<NotificationResponse> {
    const res = await api.patch<NotificationResponse>(`/api/v1/notifications/${id}/read`);
    return res.data;
  },

  async markAllNotificationsAsRead(): Promise<void> {
    await api.patch('/api/v1/notifications/read-all');
  },
};
