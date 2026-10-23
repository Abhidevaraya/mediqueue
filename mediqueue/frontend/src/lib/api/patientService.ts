import { api } from '@/lib/api/client';
import { PatientProfileResponse, PatientProfileUpdateRequest } from '@/types/patient';

export const patientService = {
  async getMyProfile(): Promise<PatientProfileResponse> {
    const res = await api.get<PatientProfileResponse>('/api/v1/patients/me');
    return res.data;
  },

  async updateMyProfile(data: PatientProfileUpdateRequest): Promise<PatientProfileResponse> {
    const res = await api.put<PatientProfileResponse>('/api/v1/patients/me', data);
    return res.data;
  },
};
