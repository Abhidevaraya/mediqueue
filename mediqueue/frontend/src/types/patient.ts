export interface PatientProfileResponse {
  id: number;
  userId: number;
  fullName: string;
  email: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  gender?: string;
  address?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PatientProfileUpdateRequest {
  fullName?: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  gender?: string;
  address?: string;
}
