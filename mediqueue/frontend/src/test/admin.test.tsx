import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AdminDashboardOverviewPage } from '@/features/admin/AdminDashboardOverviewPage';
import { AdminUserDirectoryPage } from '@/features/admin/AdminUserDirectoryPage';
import { AdminDoctorsPage } from '@/features/admin/AdminDoctorsPage';
import { AdminDepartmentsPage } from '@/features/admin/AdminDepartmentsPage';
import { AdminAnalyticsPage } from '@/features/admin/AdminAnalyticsPage';
import { AdminAuditLogsPage } from '@/features/admin/AdminAuditLogsPage';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { ToastProvider } from '@/lib/feedback/ToastContext';
import { ThemeProvider } from '@/lib/theme/ThemeContext';
import { adminService } from '@/lib/api/adminService';
import { authService } from '@/lib/auth/authService';
import { tokenStorage } from '@/lib/auth/tokenStorage';

vi.mock('@/lib/api/adminService', () => ({
  adminService: {
    getOverview: vi.fn(),
    getTodayQueues: vi.fn(),
    pauseQueue: vi.fn(),
    resumeQueue: vi.fn(),
    getUsers: vi.fn(),
    getUserById: vi.fn(),
    updateUserStatus: vi.fn(),
    updateUserRole: vi.fn(),
    getDoctors: vi.fn(),
    getDoctorById: vi.fn(),
    createDoctor: vi.fn(),
    updateDoctor: vi.fn(),
    updateDoctorVerification: vi.fn(),
    updateDoctorStatus: vi.fn(),
    getDepartments: vi.fn(),
    createDepartment: vi.fn(),
    updateDepartment: vi.fn(),
    updateDepartmentStatus: vi.fn(),
    getSchedules: vi.fn(),
    getAppointmentAnalytics: vi.fn(),
    getWaitTimeAnalytics: vi.fn(),
    getDoctorWorkload: vi.fn(),
    getDepartmentWorkload: vi.fn(),
    exportAppointmentsCsv: vi.fn(),
    exportWaitTimesCsv: vi.fn(),
    getAuditLogs: vi.fn(),
    exportAuditLogsCsv: vi.fn(),
  },
}));

vi.mock('@/lib/auth/authService', () => ({
  authService: {
    getCurrentUser: vi.fn(),
  },
}));

const renderWithProviders = (ui: React.ReactElement) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <ThemeProvider>
      <ToastProvider>
        <AuthProvider>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter>{ui}</MemoryRouter>
          </QueryClientProvider>
        </AuthProvider>
      </ToastProvider>
    </ThemeProvider>
  );
};

describe('Phase 3: Admin Dashboard & Operational Console Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(tokenStorage, 'getToken').mockReturnValue('mock-admin-jwt');
    vi.mocked(authService.getCurrentUser).mockResolvedValue({
      id: 99,
      fullName: 'Super Admin',
      email: 'admin@mediqueue.org',
      role: 'ADMIN',
      accountStatus: 'ACTIVE',
    });
  });

  it('renders hospital overview metrics and today queue sessions', async () => {
    vi.mocked(adminService.getOverview).mockResolvedValue({
      totalAppointmentsToday: 42,
      completedAppointmentsToday: 28,
      activeQueueSessionsToday: 4,
      waitingPatientsToday: 9,
      averageWaitTimeMinutesToday: 14,
      averageConsultationDurationMinutesToday: 11,
    });

    vi.mocked(adminService.getTodayQueues).mockResolvedValue([
      {
        sessionId: 101,
        doctorId: 5,
        doctorName: 'Dr. Gregory House',
        departmentName: 'Diagnostics',
        sessionDate: '2026-10-13',
        isPaused: false,
        currentCalledToken: 12,
        currentInProgressToken: 11,
        lastTokenNumber: 15,
        waitingCount: 3,
        calledCount: 1,
        inProgressCount: 1,
        skippedCount: 0,
        completedCount: 10,
        cancelledCount: 0,
        totalPatients: 15,
      },
    ]);

    renderWithProviders(<AdminDashboardOverviewPage />);

    await waitFor(() => {
      expect(screen.getByText('Hospital Administration Overview')).toBeInTheDocument();
      expect(screen.getByText('42')).toBeInTheDocument(); // total appointments
      expect(screen.getByText('Dr. Gregory House')).toBeInTheDocument();
      expect(screen.getByText('Diagnostics')).toBeInTheDocument();
      expect(screen.getByText('3 waiting')).toBeInTheDocument();
    });
  });

  it('renders user directory and updates user account status', async () => {
    vi.mocked(adminService.getUsers).mockResolvedValue({
      content: [
        {
          id: 1,
          fullName: 'Jane Doe',
          email: 'jane@example.com',
          phoneNumber: '+1-555-0100',
          role: 'PATIENT',
          accountStatus: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      pageSize: 15,
      pageNumber: 0,
      first: true,
      last: true,
      empty: false,
    });

    vi.mocked(adminService.updateUserStatus).mockResolvedValue({
      id: 1,
      fullName: 'Jane Doe',
      email: 'jane@example.com',
      phoneNumber: '+1-555-0100',
      role: 'PATIENT',
      accountStatus: 'BLOCKED',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-02T00:00:00Z',
    });

    renderWithProviders(<AdminUserDirectoryPage />);

    await waitFor(() => {
      expect(screen.getByText('User Directory & Accounts')).toBeInTheDocument();
      expect(screen.getByText('Jane Doe')).toBeInTheDocument();
      expect(screen.getByText('jane@example.com')).toBeInTheDocument();
    });

    // Click block button
    const blockButton = screen.getByTitle('Block User Account');
    fireEvent.click(blockButton);

    await waitFor(() => {
      expect(screen.getByText(/Confirm Account Status Change/i)).toBeInTheDocument();
    });

    const confirmBtn = screen.getByText('Confirm BLOCKED');
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(adminService.updateUserStatus).toHaveBeenCalledWith(1, 'BLOCKED');
    });
  });

  it('renders doctor management and opens doctor onboarding dialog', async () => {
    vi.mocked(adminService.getDepartments).mockResolvedValue([
      { id: 1, name: 'Cardiology', code: 'CARD', description: 'Heart care', isActive: true },
    ]);

    vi.mocked(adminService.getDoctors).mockResolvedValue({
      content: [
        {
          id: 10,
          userId: 20,
          fullName: 'Dr. Sarah Connor',
          email: 'sarah@hospital.org',
          departmentId: 1,
          departmentName: 'Cardiology',
          specialty: 'Cardiovascular Surgeon',
          qualification: 'MD, FACC',
          yearsOfExperience: 12,
          consultationFee: 150.0,
          verificationStatus: 'VERIFIED',
          accountStatus: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      pageSize: 50,
      pageNumber: 0,
      first: true,
      last: true,
      empty: false,
    });

    renderWithProviders(<AdminDoctorsPage />);

    await waitFor(() => {
      expect(screen.getByText('Doctor Management & Credentialing')).toBeInTheDocument();
      expect(screen.getByText('Dr. Sarah Connor')).toBeInTheDocument();
      expect(screen.getByText('Cardiovascular Surgeon')).toBeInTheDocument();
    });

    // Open onboard modal
    const onboardBtn = screen.getByText('Onboard Doctor');
    fireEvent.click(onboardBtn);

    await waitFor(() => {
      expect(screen.getByText('Onboard Medical Specialist')).toBeInTheDocument();
    });
  });

  it('renders department catalog and manages department active status', async () => {
    vi.mocked(adminService.getDepartments).mockResolvedValue([
      { id: 1, name: 'Neurology', code: 'NEUR', description: 'Brain & Spine Unit', isActive: true },
    ]);

    vi.mocked(adminService.updateDepartmentStatus).mockResolvedValue({
      id: 1,
      name: 'Neurology',
      code: 'NEUR',
      description: 'Brain & Spine Unit',
      isActive: false,
    });

    renderWithProviders(<AdminDepartmentsPage />);

    await waitFor(() => {
      expect(screen.getByText('Department Catalog')).toBeInTheDocument();
      expect(screen.getByText('Neurology')).toBeInTheDocument();
      expect(screen.getByText('Brain & Spine Unit')).toBeInTheDocument();
    });

    const deactBtn = screen.getByText('Deactivate');
    fireEvent.click(deactBtn);

    await waitFor(() => {
      expect(screen.getByText('Deactivate Department?')).toBeInTheDocument();
    });
  });

  it('renders operational analytics, attendance rates, and wait times', async () => {
    vi.mocked(adminService.getAppointmentAnalytics).mockResolvedValue({
      totalAppointments: 100,
      bookedCount: 15,
      checkedInCount: 10,
      inProgressCount: 5,
      completedCount: 65,
      cancelledCount: 5,
      noShowCount: 0,
      attendanceRatePercent: 93,
      cancellationRatePercent: 5,
      dailyBreakdown: [
        { date: '2026-10-12', total: 50, completed: 35, cancelled: 3, booked: 12 },
      ],
    });

    vi.mocked(adminService.getWaitTimeAnalytics).mockResolvedValue({
      totalSamples: 70,
      averageWaitTimeMinutes: 12,
      medianWaitTimeMinutes: 10,
      p90WaitTimeMinutes: 22,
      minWaitTimeMinutes: 2,
      maxWaitTimeMinutes: 38,
      meanAbsolutePredictionErrorMinutes: 3,
      predictionMethodBreakdown: { 'HISTORICAL_REGRESSION': 50, 'MOVING_AVERAGE': 20 },
    });

    vi.mocked(adminService.getDoctorWorkload).mockResolvedValue([
      {
        doctorId: 1,
        doctorName: 'Dr. Allison Cameron',
        specialty: 'Immunology',
        departmentName: 'Diagnostics',
        totalAppointments: 25,
        completedConsultations: 20,
        averageConsultationMinutes: 14,
        medianConsultationMinutes: 12,
      },
    ]);

    vi.mocked(adminService.getDepartmentWorkload).mockResolvedValue([
      {
        departmentId: 1,
        departmentName: 'Diagnostics',
        totalAppointments: 60,
        completedAppointments: 45,
        cancelledAppointments: 5,
        activeDoctorsCount: 3,
      },
    ]);

    renderWithProviders(<AdminAnalyticsPage />);

    await waitFor(() => {
      expect(screen.getByText('Clinical Operations & Queue Analytics')).toBeInTheDocument();
      expect(screen.getByText('93%')).toBeInTheDocument(); // Attendance rate
      expect(screen.getByText('12m')).toBeInTheDocument(); // Avg wait time
      expect(screen.getByText('Dr. Allison Cameron')).toBeInTheDocument();
      expect(screen.getAllByText('Diagnostics').length).toBeGreaterThan(0);
    });
  });

  it('renders clinical audit trail with compliance records', async () => {
    vi.mocked(adminService.getAuditLogs).mockResolvedValue({
      content: [
        {
          id: 501,
          actorUserId: 12,
          actorRole: 'DOCTOR',
          action: 'CONSULTATION_COMPLETED',
          resourceType: 'CONSULTATION',
          resourceId: 88,
          patientId: 3,
          outcome: 'SUCCESS',
          safeReason: 'Clinical notes documented and prescribed',
          eventTimestamp: '2026-10-13T10:30:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      pageSize: 20,
      pageNumber: 0,
      first: true,
      last: true,
      empty: false,
    });

    renderWithProviders(<AdminAuditLogsPage />);

    await waitFor(() => {
      expect(screen.getByText('Clinical System Audit Trail')).toBeInTheDocument();
      expect(screen.getByText('CONSULTATION_COMPLETED')).toBeInTheDocument();
      expect(screen.getAllByText('SUCCESS').length).toBeGreaterThan(0);
    });
  });
});
