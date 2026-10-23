export type UserRole = 'PATIENT' | 'DOCTOR' | 'ADMIN';

export type AccountStatus = 'ACTIVE' | 'BLOCKED' | 'PENDING';

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  accountStatus: AccountStatus;
  phoneNumber?: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: UserSummary;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterPayload {
  fullName: string;
  email: string;
  password: string;
  phoneNumber?: string;
}

export interface DoctorRegisterPayload {
  fullName: string;
  email: string;
  password: string;
  phoneNumber?: string;
  departmentId: number;
  specialty: string;
  qualification: string;
  yearsOfExperience: number;
  consultationFee: number;
  professionalBio?: string;
}
