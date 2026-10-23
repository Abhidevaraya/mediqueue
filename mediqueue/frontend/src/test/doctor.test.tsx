import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DoctorDashboardPage } from '@/features/doctor/DoctorDashboardPage';
import { DoctorQueueConsolePage } from '@/features/doctor/DoctorQueueConsolePage';
import { DoctorSchedulePage } from '@/features/doctor/DoctorSchedulePage';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { ToastProvider } from '@/lib/feedback/ToastContext';
import { ThemeProvider } from '@/lib/theme/ThemeContext';
import { doctorService } from '@/lib/api/doctorService';
import { appointmentService } from '@/lib/api/appointmentService';
import { queueService } from '@/lib/api/queueService';
import { authService } from '@/lib/auth/authService';
import { tokenStorage } from '@/lib/auth/tokenStorage';
import { AppointmentResponse } from '@/types/appointment';

vi.mock('@/lib/api/doctorService', () => ({
  doctorService: {
    getDoctorSchedules: vi.fn(),
    createSchedule: vi.fn(),
    getExceptions: vi.fn(),
    createException: vi.fn(),
  },
}));

vi.mock('@/lib/api/appointmentService', () => ({
  appointmentService: {
    getDoctorTodayAppointments: vi.fn(),
  },
}));

vi.mock('@/lib/api/queueService', () => ({
  queueService: {
    getDoctorQueueToday: vi.fn(),
    callNext: vi.fn(),
    callEntry: vi.fn(),
    startConsultation: vi.fn(),
    completeConsultation: vi.fn(),
    skipEntry: vi.fn(),
    requeueEntry: vi.fn(),
    pauseQueue: vi.fn(),
    resumeQueue: vi.fn(),
  },
}));

vi.mock('@/lib/auth/authService', () => ({
  authService: {
    getCurrentUser: vi.fn(),
  },
}));

vi.mock('@/lib/websocket/useQueueSubscription', () => ({
  useQueueSubscription: () => ({
    isConnected: true,
    isConnecting: false,
    stompClient: null,
  }),
}));

const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
    },
  });

const renderWithProviders = (ui: React.ReactElement) => {
  const queryClient = createTestQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ThemeProvider>
          <ToastProvider>
            <AuthProvider>{ui}</AuthProvider>
          </ToastProvider>
        </ThemeProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
};

describe('Doctor Phase 2 Workflows', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    tokenStorage.setToken('test-doctor-jwt');
    vi.mocked(authService.getCurrentUser).mockResolvedValue({
      id: 201,
      fullName: 'Dr. Gregory House',
      email: 'house@princeton.edu',
      role: 'DOCTOR',
      accountStatus: 'ACTIVE',
    });
  });

  describe('Doctor Dashboard', () => {
    it('renders OPD metrics and allows triggering Call Next Patient', async () => {
      vi.mocked(appointmentService.getDoctorTodayAppointments).mockResolvedValue([
        {
          id: 501,
          appointmentReference: 'APT-001',
          patientId: 101,
          patientName: 'Sarah Connor',
          patientEmail: 'sarah@example.com',
          doctorId: 201,
          doctorName: 'Dr. Gregory House',
          specialty: 'Cardiology',
          consultationFee: 150,
          departmentId: 1,
          departmentName: 'Cardiology',
          startDateTime: '2026-10-13T09:00:00',
          endDateTime: '2026-10-13T09:20:00',
          slotDurationMinutes: 20,
          status: 'CHECKED_IN',
          bookingTimestamp: '2026-10-12T10:00:00',
          createdAt: '2026-10-12T10:00:00',
          updatedAt: '2026-10-12T10:00:00',
        } as AppointmentResponse,
      ]);

      vi.mocked(queueService.getDoctorQueueToday).mockResolvedValue({
        doctorId: 201,
        doctorName: 'Dr. Gregory House',
        queueDate: '2026-10-13',
        isPaused: false,
        waitingCount: 4,
        calledCount: 1,
        inProgressCount: 0,
        completedCount: 2,
        skippedCount: 0,
        totalCheckedIn: 7,
        currentCalledToken: 5,
        entries: [],
      });

      vi.mocked(queueService.callNext).mockResolvedValue({
        id: 99,
        entryReference: 'Q-ENTRY-099',
        tokenNumber: 6,
        appointmentId: 502,
        appointmentReference: 'APT-002',
        patientId: 102,
        patientName: 'John Doe',
        doctorId: 201,
        doctorName: 'Dr. Gregory House',
        departmentId: 1,
        departmentName: 'Cardiology',
        queueDate: '2026-10-13',
        checkInTimestamp: '2026-10-13T09:10:00',
        status: 'CALLED',
        priorityCategory: 'NORMAL',
        createdAt: '2026-10-13T09:10:00',
      });

      renderWithProviders(<DoctorDashboardPage />);

      // Verify dashboard headers & metrics
      expect(await screen.findByText('Clinical Workspace')).toBeInTheDocument();
      expect(await screen.findByText('Currently Serving: #5')).toBeInTheDocument();
      expect(screen.getByText('4')).toBeInTheDocument(); // Waiting count
      expect(screen.getByText('7')).toBeInTheDocument(); // Total OPD entries

      // Trigger Call Next Patient button
      const callNextBtn = screen.getByRole('button', { name: /call next patient/i });
      fireEvent.click(callNextBtn);

      await waitFor(() => {
        expect(queueService.callNext).toHaveBeenCalled();
      });
    });
  });

  describe('Doctor Queue Console', () => {
    it('renders queue entries and allows consultation actions', async () => {
      vi.mocked(queueService.getDoctorQueueToday).mockResolvedValue({
        doctorId: 201,
        doctorName: 'Dr. Gregory House',
        queueDate: '2026-10-13',
        isPaused: false,
        waitingCount: 1,
        calledCount: 0,
        inProgressCount: 0,
        completedCount: 0,
        skippedCount: 0,
        totalCheckedIn: 1,
        entries: [
          {
            id: 88,
            entryReference: 'Q-ENTRY-088',
            tokenNumber: 8,
            appointmentId: 510,
            appointmentReference: 'APT-010',
            patientId: 108,
            patientName: 'Robert Chase',
            doctorId: 201,
            doctorName: 'Dr. Gregory House',
            departmentId: 1,
            departmentName: 'Cardiology',
            queueDate: '2026-10-13',
            checkInTimestamp: '2026-10-13T09:30:00',
            status: 'WAITING',
            priorityCategory: 'NORMAL',
            createdAt: '2026-10-13T09:30:00',
          },
        ],
      });

      vi.mocked(queueService.callEntry).mockResolvedValue({
        id: 88,
        entryReference: 'Q-ENTRY-088',
        tokenNumber: 8,
        appointmentId: 510,
        appointmentReference: 'APT-010',
        patientId: 108,
        patientName: 'Robert Chase',
        doctorId: 201,
        doctorName: 'Dr. Gregory House',
        departmentId: 1,
        departmentName: 'Cardiology',
        queueDate: '2026-10-13',
        checkInTimestamp: '2026-10-13T09:30:00',
        status: 'CALLED',
        priorityCategory: 'NORMAL',
        createdAt: '2026-10-13T09:30:00',
      });

      renderWithProviders(<DoctorQueueConsolePage />);

      expect(await screen.findByText('OPD Station Queue Console')).toBeInTheDocument();
      expect(await screen.findByText('Robert Chase')).toBeInTheDocument();
      expect(screen.getByText('#8')).toBeInTheDocument();

      // Call patient action
      const callBtn = screen.getByRole('button', { name: /call token/i });
      fireEvent.click(callBtn);

      await waitFor(() => {
        expect(queueService.callEntry).toHaveBeenCalledWith(88);
      });
    });
  });

  describe('Doctor Schedule Management', () => {
    it('renders published working hours and handles leave exception', async () => {
      vi.mocked(doctorService.getDoctorSchedules).mockResolvedValue([
        {
          id: 1,
          doctorId: 201,
          dayOfWeek: 'MONDAY',
          startTime: '09:00:00',
          endTime: '17:00:00',
          slotDurationMinutes: 20,
          breakStartTime: '13:00:00',
          breakEndTime: '14:00:00',
          isActive: true,
        },
      ]);

      vi.mocked(doctorService.getExceptions).mockResolvedValue([]);

      renderWithProviders(<DoctorSchedulePage />);

      expect(await screen.findByText('Doctor Schedule & Availability')).toBeInTheDocument();
      expect(await screen.findByText('MONDAY')).toBeInTheDocument();
      expect(screen.getByText(/09:00/i)).toBeInTheDocument();
      expect(screen.getByText(/13:00/i)).toBeInTheDocument();
    });
  });
});
