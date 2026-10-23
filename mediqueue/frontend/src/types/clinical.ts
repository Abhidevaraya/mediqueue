export type ConsultationStatus =
  | 'WAITING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface ConsultationResponse {
  id: number;
  consultationReference: string;
  appointmentId: number;
  appointmentReference: string;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  status: ConsultationStatus;
  actualStartTimestamp?: string;
  actualEndTimestamp?: string;
  durationMinutes?: number;
  cancellationReason?: string;
  createdAt: string;
}

export type VisitSummaryStatus = 'DRAFT' | 'APPROVED';

export interface VisitSummaryResponse {
  id: number;
  summaryReference: string;
  consultationId: number;
  consultationReference: string;
  appointmentId: number;
  appointmentReference: string;
  patientId: number;
  patientName: string;
  authoringDoctorId: number;
  authoringDoctorName: string;
  chiefComplaint?: string;
  diagnosis?: string;
  clinicalNotes?: string;
  treatmentPlan?: string;
  status: VisitSummaryStatus;
  approvedAt?: string;
  approvedByDoctorId?: number;
  versionNumber: number;
  revisionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface VisitSummaryCreateRequest {
  consultationId: number;
  chiefComplaint?: string;
  diagnosis?: string;
  clinicalNotes?: string;
  treatmentPlan?: string;
}

export interface VisitSummaryUpdateRequest {
  chiefComplaint?: string;
  diagnosis?: string;
  clinicalNotes?: string;
  treatmentPlan?: string;
}

export interface VisitSummaryRevisionRequest {
  revisionReason: string;
  chiefComplaint?: string;
  diagnosis?: string;
  clinicalNotes?: string;
  treatmentPlan?: string;
}

export type PrescriptionStatus = 'ISSUED' | 'CANCELLED';

export interface PrescriptionItemResponse {
  id: number;
  medicationName: string;
  dosage: string;
  frequency: string;
  durationDays: number;
  route?: string;
  instructions?: string;
  itemOrder?: number;
}

export interface PrescriptionResponse {
  id: number;
  prescriptionReference: string;
  consultationId: number;
  consultationReference: string;
  appointmentId: number;
  appointmentReference: string;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  status: PrescriptionStatus;
  issuedAt: string;
  cancelledAt?: string;
  cancellationReason?: string;
  notes?: string;
  versionNumber: number;
  parentPrescriptionId?: number;
  items: PrescriptionItemResponse[];
  createdAt: string;
}

export interface PrescriptionItemRequest {
  medicationName: string;
  dosage: string;
  frequency: string;
  durationDays: number;
  route?: string;
  instructions?: string;
}

export interface PrescriptionCreateRequest {
  consultationId: number;
  notes?: string;
  items: PrescriptionItemRequest[];
}

export interface PrescriptionRevisionRequest {
  revisionReason: string;
  notes?: string;
  items: PrescriptionItemRequest[];
}

export type FollowUpStatus = 'RECOMMENDED' | 'BOOKED' | 'COMPLETED' | 'CANCELLED';
export type FollowUpReminderStatus = 'PENDING' | 'SENT' | 'FAILED' | 'DISMISSED';

export interface FollowUpResponse {
  id: number;
  followUpReference: string;
  consultationId: number;
  consultationReference: string;
  appointmentId: number;
  appointmentReference: string;
  patientId: number;
  patientName: string;
  doctorId: number;
  doctorName: string;
  recommendedDate: string; // YYYY-MM-DD
  instructions?: string;
  status: FollowUpStatus;
  reminderStatus: FollowUpReminderStatus;
  reminderSentAt?: string;
  completedAt?: string;
  cancellationReason?: string;
  createdAt: string;
}

export interface FollowUpCreateRequest {
  consultationId: number;
  recommendedDate: string; // YYYY-MM-DD
  instructions?: string;
}

export type DocumentCategory =
  | 'LAB_REPORT'
  | 'IMAGING'
  | 'DISCHARGE_SUMMARY'
  | 'PRESCRIPTION_SCAN'
  | 'OTHER';

export interface MedicalDocumentResponse {
  id: number;
  documentReference: string;
  patientId: number;
  patientName: string;
  appointmentId?: number;
  consultationId?: number;
  uploadedByUserId: number;
  documentCategory: DocumentCategory;
  originalFileName: string;
  contentType: string;
  fileSizeBytes: number;
  checksumSha256: string;
  createdAt: string;
}

export interface NotificationResponse {
  id: number;
  recipientUserId: number;
  title: string;
  message: string;
  eventType: string;
  isRead: boolean;
  readAt?: string;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}
