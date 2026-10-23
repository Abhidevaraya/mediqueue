import { UserRole } from '@/types/auth';

export type AccountStatus = 'ACTIVE' | 'BLOCKED' | 'PENDING';
export type DoctorVerificationStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export interface QueueSessionResponse {
  id: number;
  doctorId: number;
  queueDate: string; // YYYY-MM-DD
  lastTokenNumber: number;
  isPaused: boolean;
  pausedAt?: string;
  pauseReason?: string;
}

export interface HospitalOverviewResponse {
  totalAppointmentsToday: number;
  completedAppointmentsToday: number;
  activeQueueSessionsToday: number;
  waitingPatientsToday: number;
  averageWaitTimeMinutesToday: number;
  averageConsultationDurationMinutesToday: number;
}

export interface DailyAppointmentMetric {
  date: string; // YYYY-MM-DD
  total: number;
  completed: number;
  cancelled: number;
  booked: number;
}

export interface AppointmentAnalyticsResponse {
  fromDate?: string;
  toDate?: string;
  totalAppointments: number;
  bookedCount: number;
  checkedInCount: number;
  inProgressCount: number;
  completedCount: number;
  cancelledCount: number;
  noShowCount: number;
  attendanceRatePercent: number;
  cancellationRatePercent: number;
  dailyBreakdown: DailyAppointmentMetric[];
}

export interface WaitTimeAnalyticsResponse {
  fromDate?: string;
  toDate?: string;
  totalSamples: number;
  averageWaitTimeMinutes: number;
  medianWaitTimeMinutes: number;
  p90WaitTimeMinutes: number;
  minWaitTimeMinutes: number;
  maxWaitTimeMinutes: number;
  meanAbsolutePredictionErrorMinutes: number;
  predictionMethodBreakdown: Record<string, number>;
}

export interface DoctorWorkloadResponse {
  doctorId: number;
  doctorName: string;
  specialty: string;
  departmentName: string;
  totalAppointments: number;
  completedConsultations: number;
  averageConsultationMinutes: number;
  medianConsultationMinutes: number;
}

export interface DepartmentWorkloadResponse {
  departmentId: number;
  departmentName: string;
  totalAppointments: number;
  completedAppointments: number;
  cancelledAppointments: number;
  activeDoctorsCount: number;
}

export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  phoneNumber?: string;
  role: UserRole;
  accountStatus: AccountStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DoctorAdminResponse {
  id: number;
  userId: number;
  fullName: string;
  email: string;
  phoneNumber?: string;
  departmentId: number;
  departmentName: string;
  specialty: string;
  qualification: string;
  yearsOfExperience: number;
  professionalBio?: string;
  consultationFee: number;
  verificationStatus: DoctorVerificationStatus;
  accountStatus: AccountStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DoctorCreateRequest {
  fullName: string;
  email: string;
  password?: string;
  phoneNumber?: string;
  departmentId: number;
  specialty: string;
  qualification: string;
  yearsOfExperience: number;
  professionalBio?: string;
  consultationFee: number;
  verificationStatus?: DoctorVerificationStatus;
}

export interface DoctorUpdateRequest {
  specialty?: string;
  qualification?: string;
  yearsOfExperience?: number;
  professionalBio?: string;
  consultationFee?: number;
  departmentId?: number;
}

export interface DepartmentCreateRequest {
  name: string;
  description?: string;
}

export interface DepartmentUpdateRequest {
  name: string;
  description?: string;
  active?: boolean;
}

export interface AdminQueueSummaryResponse {
  sessionId: number;
  doctorId: number;
  doctorName: string;
  departmentId?: number;
  departmentName: string;
  sessionDate: string; // YYYY-MM-DD
  isPaused: boolean;
  currentCalledToken?: number;
  currentInProgressToken?: number;
  lastTokenNumber: number;
  waitingCount: number;
  calledCount: number;
  inProgressCount: number;
  skippedCount: number;
  completedCount: number;
  cancelledCount: number;
  totalPatients: number;
}

export interface ClinicalAuditEvent {
  id: number;
  actorId?: number;
  actorUserId?: number;
  actorRole?: string;
  action: string;
  resourceType: string;
  resourceId?: number;
  patientId?: number;
  outcome: string;
  safeReason?: string;
  eventTimestamp: string;
}
