import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { DoctorDirectoryPage } from '@/features/doctors/DoctorDirectoryPage';
import { PatientLiveQueuePage } from '@/features/queue/PatientLiveQueuePage';
import { PatientAppointmentsPage } from '@/features/patient/PatientAppointmentsPage';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { ToastProvider } from '@/lib/feedback/ToastContext';
import { ThemeProvider } from '@/lib/theme/ThemeContext';
import { doctorService } from '@/lib/api/doctorService';
import { appointmentService } from '@/lib/api/appointmentService';
import { queueService } from '@/lib/api/queueService';
import { authService } from '@/lib/auth/authService';
import { tokenStorage } from '@/lib/auth/tokenStorage';
import { AppointmentResponse } from '@/types/appointment';

// Mock API services
vi.mock('@/lib/api/doctorService', () => ({
  doctorService: {
    getDepartments: vi.fn(),
    getDoctors: vi.fn(),
    getDoctorById: vi.fn(),
    getAvailableSlots: vi.fn(),
  },
}));

vi.mock('@/lib/api/appointmentService', () => ({
  appointmentService: {
    bookAppointment: vi.fn(),
    getMyAppointments: vi.fn(),
    getAppointmentById: vi.fn(),
    cancelAppointment: vi.fn(),
    rescheduleAppointment: vi.fn(),
  },
}));

vi.mock('@/lib/api/queueService', () => ({
  queueService: {
    checkInAppointment: vi.fn(),
    getMyActiveQueueStatus: vi.fn(),
    getMyWaitTimeEstimate: vi.fn(),
    getQueueEvents: vi.fn(),
  },
}));

vi.mock('@/lib/auth/authService', () => ({
  authService: {
    getCurrentUser: vi.fn(),
  },
}));

// Mock WebSocket hook to return connected status
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

describe('Patient Phase 2 Workflows', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    tokenStorage.setToken('test-patient-jwt');
    vi.mocked(authService.getCurrentUser).mockResolvedValue({
      id: 101,
      fullName: 'Sarah Connor',
      email: 'sarah@example.com',
      role: 'PATIENT',
      accountStatus: 'ACTIVE',
    });
  });

  describe('Doctor Directory and Availability', () => {
    it('renders doctor cards with department filters and triggers booking modal', async () => {
      vi.mocked(doctorService.getDepartments).mockResolvedValue({
        content: [
          {
            id: 1,
            code: 'CARDIO',
            name: 'Cardiology',
            description: 'Heart and vascular care',
            isActive: true,
          },
        ],
        pageNumber: 0,
        pageSize: 50,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        empty: false,
      });

      vi.mocked(doctorService.getDoctors).mockResolvedValue({
        content: [
          {
            id: 201,
            fullName: 'Dr. Gregory House',
            specialty: 'Diagnostic Cardiology',
            departmentId: 1,
            departmentName: 'Cardiology',
            qualification: 'MD, FACC',
            yearsOfExperience: 18,
            professionalBio: 'Expert diagnostics and cardiology.',
            consultationFee: 150.0,
          },
        ],
        pageNumber: 0,
        pageSize: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        empty: false,
      });

      vi.mocked(doctorService.getAvailableSlots).mockResolvedValue([
        {
          doctorId: 201,
          date: '2026-10-20',
          startTime: '09:00:00',
          endTime: '09:20:00',
          durationMinutes: 20,
          available: true,
        },
      ]);

      renderWithProviders(<DoctorDirectoryPage />);

      // Verify doctor info displayed
      expect(await screen.findByText('Dr. Gregory House')).toBeInTheDocument();
      expect(screen.getByText('Diagnostic Cardiology')).toBeInTheDocument();
      expect(screen.getByText(/\$150/)).toBeInTheDocument();

      // Open booking modal
      const bookButton = screen.getByRole('button', { name: /check slots & book/i });
      fireEvent.click(bookButton);

      // Verify booking modal is rendered
      expect(await screen.findByRole('heading', { name: /book doctor consultation/i })).toBeInTheDocument();
      expect(screen.getByText(/Diagnostic Cardiology • Cardiology/i)).toBeInTheDocument();
    });
  });

  describe('Patient Live Queue Page', () => {
    it('displays active digital queue token and estimated wait time', async () => {
      vi.mocked(queueService.getMyActiveQueueStatus).mockResolvedValue({
        id: 777,
        entryReference: 'Q-ENTRY-001',
        tokenNumber: 42,
        appointmentId: 501,
        appointmentReference: 'APT-20261013-0001',
        patientId: 101,
        patientName: 'Sarah Connor',
        doctorId: 201,
        doctorName: 'Dr. Gregory House',
        departmentId: 1,
        departmentName: 'Cardiology',
        queueDate: '2026-10-13',
        checkInTimestamp: '2026-10-13T08:30:00',
        status: 'WAITING',
        priorityCategory: 'NORMAL',
        createdAt: '2026-10-13T08:30:00',
      });

      vi.mocked(queueService.getMyWaitTimeEstimate).mockResolvedValue({
        queueEntryId: 777,
        tokenNumber: 42,
        eligiblePatientsAhead: 2,
        estimatedWaitMinutes: 25,
        lowerBoundMinutes: 20,
        upperBoundMinutes: 30,
        predictionMethod: 'HISTORICAL_AVERAGE',
        usesFallback: false,
        queuePaused: false,
        lastUpdatedAt: '2026-10-13T09:00:00',
      });

      vi.mocked(queueService.getQueueEvents).mockResolvedValue([]);

      renderWithProviders(<PatientLiveQueuePage />);

      // Verify token number displayed prominently
      expect(await screen.findByText('#42')).toBeInTheDocument();
      expect(await screen.findByText(/25 mins/)).toBeInTheDocument(); // Wait time estimate
      expect(screen.getByText('Real-Time Connected')).toBeInTheDocument(); // STOMP connection badge
    });
  });

  describe('Patient Appointments Management', () => {
    it('renders appointments list and allows checking in to receive queue token', async () => {
      const todayStr = new Date().toISOString().split('T')[0];
      vi.mocked(appointmentService.getMyAppointments).mockResolvedValue({
        content: [
          {
            id: 501,
            appointmentReference: 'APT-20261013-0001',
            patientId: 101,
            patientName: 'Sarah Connor',
            patientEmail: 'sarah@example.com',
            doctorId: 201,
            doctorName: 'Dr. Gregory House',
            specialty: 'Cardiology',
            consultationFee: 150,
            departmentId: 1,
            departmentName: 'Cardiology',
            startDateTime: `${todayStr}T10:00:00`,
            endDateTime: `${todayStr}T10:20:00`,
            slotDurationMinutes: 20,
            status: 'BOOKED',
            reason: 'Routine ECG Followup',
            bookingTimestamp: '2026-10-10T09:00:00',
            createdAt: '2026-10-10T09:00:00',
            updatedAt: '2026-10-10T09:00:00',
          } as AppointmentResponse,
        ],
        pageNumber: 0,
        pageSize: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        empty: false,
      });

      vi.mocked(queueService.checkInAppointment).mockResolvedValue({
        id: 777,
        entryReference: 'Q-ENTRY-001',
        tokenNumber: 42,
        appointmentId: 501,
        appointmentReference: 'APT-20261013-0001',
        patientId: 101,
        patientName: 'Sarah Connor',
        doctorId: 201,
        doctorName: 'Dr. Gregory House',
        departmentId: 1,
        departmentName: 'Cardiology',
        queueDate: todayStr,
        checkInTimestamp: `${todayStr}T09:45:00`,
        status: 'WAITING',
        priorityCategory: 'NORMAL',
        createdAt: `${todayStr}T09:45:00`,
      });

      renderWithProviders(<PatientAppointmentsPage />);

      // Verify appointment is listed
      expect(await screen.findByText(/APT-20261013-0001/)).toBeInTheDocument();
      expect(screen.getByText('Dr. Gregory House')).toBeInTheDocument();
      expect(screen.getByText(/Routine ECG Followup/)).toBeInTheDocument();

      // Check-in button is available for today's BOOKED appointments
      const checkInBtn = screen.getByRole('button', { name: /check in now/i });
      fireEvent.click(checkInBtn);

      await waitFor(() => {
        expect(queueService.checkInAppointment).toHaveBeenCalledWith(501, expect.anything());
      });
    });
  });
});
