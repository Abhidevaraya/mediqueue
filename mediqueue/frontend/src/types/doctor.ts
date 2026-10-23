export interface DepartmentResponse {
  id: number;
  name: string;
  code?: string;
  description?: string;
  active?: boolean;
  isActive?: boolean;
}

export interface DoctorPublicResponse {
  id: number;
  fullName: string;
  specialty: string;
  departmentId: number;
  departmentName: string;
  qualification: string;
  yearsOfExperience: number;
  professionalBio?: string;
  consultationFee: number;
}

export interface SlotResponse {
  doctorId: number;
  date: string; // YYYY-MM-DD
  startTime: string; // HH:mm:ss
  endTime: string; // HH:mm:ss
  durationMinutes: number;
  available: boolean;
  unavailabilityReason?: string;
}

export interface DoctorScheduleResponse {
  id: number;
  doctorId: number;
  doctorName?: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  breakStartTime?: string;
  breakEndTime?: string;
  isActive: boolean;
}

export interface ScheduleExceptionResponse {
  id: number;
  doctorId: number;
  exceptionDate: string;
  isAvailable: boolean;
  reason?: string;
  customStartTime?: string;
  customEndTime?: string;
}
