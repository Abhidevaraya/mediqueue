export type QueueStatus =
  | 'WAITING'
  | 'CALLED'
  | 'IN_PROGRESS'
  | 'SKIPPED'
  | 'COMPLETED'
  | 'CANCELLED';

export type QueuePriority = 'NORMAL' | 'URGENT' | 'EMERGENCY';

export interface QueueEntryResponse {
  id: number;
  entryReference: string;
  appointmentId: number;
  appointmentReference: string;
  patientId: number;
  patientName: string;
  patientPhoneNumber?: string;
  doctorId: number;
  doctorName: string;
  departmentId: number;
  departmentName: string;
  queueDate: string; // YYYY-MM-DD
  tokenNumber: number;
  status: QueueStatus;
  priorityCategory: QueuePriority;
  checkInTimestamp: string;
  calledTimestamp?: string;
  consultationStartTimestamp?: string;
  consultationEndTimestamp?: string;
  skipReason?: string;
  cancellationReason?: string;
  createdAt: string;
}

export interface QueuePublicEntryResponse {
  tokenNumber: number;
  status: QueueStatus;
  calledTimestamp?: string;
}

export interface DoctorQueueSummaryResponse {
  doctorId: number;
  doctorName: string;
  queueDate: string;
  isPaused: boolean;
  pauseReason?: string;
  totalCheckedIn: number;
  waitingCount: number;
  calledCount: number;
  inProgressCount: number;
  completedCount: number;
  skippedCount: number;
  currentServingToken?: number;
  currentCalledToken?: number;
  entries: QueueEntryResponse[];
  publicEntries?: QueuePublicEntryResponse[];
}

export interface WaitTimePredictionResponse {
  queueEntryId: number;
  tokenNumber: number;
  eligiblePatientsAhead: number;
  estimatedWaitMinutes: number;
  lowerBoundMinutes?: number;
  upperBoundMinutes?: number;
  predictionMethod: string;
  usesFallback: boolean;
  queuePaused: boolean;
  lastUpdatedAt: string;
  message?: string;
}

export interface QueueRealTimeMessage {
  eventId: string;
  eventType: string;
  doctorId: number;
  queueDate: string;
  queueEntryId: number;
  tokenNumber: number;
  currentStatus: QueueStatus;
  isPaused: boolean;
  timestamp: string;
  message?: string;
}

export interface QueueEventResponse {
  id: number;
  eventId: string;
  eventType: string;
  tokenNumber: number;
  oldStatus?: QueueStatus;
  newStatus: QueueStatus;
  eventTimestamp: string;
  actorUserId?: number;
  details?: string;
}

export interface QueueSessionResponse {
  id: number;
  doctorId: number;
  queueDate: string; // YYYY-MM-DD
  lastTokenNumber: number;
  isPaused: boolean;
  pausedAt?: string;
  pauseReason?: string;
}
