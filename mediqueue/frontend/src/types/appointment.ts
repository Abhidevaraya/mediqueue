export type AppointmentStatus =
  | 'BOOKED'
  | 'CHECKED_IN'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'NO_SHOW';

export interface AppointmentResponse {
  id: number;
  appointmentReference: string;

  patientId: number;
  patientName: string;
  patientEmail: string;
  patientPhone?: string;

  doctorId: number;
  doctorName: string;
  specialty: string;
  consultationFee: number;

  departmentId: number;
  departmentName: string;

  startDateTime: string; // ISO-8601
  endDateTime: string;
  slotDurationMinutes: number;

  status: AppointmentStatus;
  reason?: string;
  bookingTimestamp: string;
  cancellationTimestamp?: string;
  cancellationReason?: string;
  rescheduledFromReference?: string;
  rescheduledTimestamp?: string;

  createdAt: string;
  updatedAt: string;
}

export interface AppointmentBookingRequest {
  doctorId: number;
  startDateTime: string; // YYYY-MM-DDTHH:mm:ss
  reason?: string;
}

export interface AppointmentCancelRequest {
  reason?: string;
}

export interface AppointmentRescheduleRequest {
  newStartDateTime: string;
  reason?: string;
}
