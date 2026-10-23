import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { LandingPage } from '@/features/landing/LandingPage';
import { LoginPage } from '@/features/auth/LoginPage';
import { RegisterPage } from '@/features/auth/RegisterPage';
import { UnauthorizedPage } from '@/features/error/UnauthorizedPage';
import { NotFoundPage } from '@/features/error/NotFoundPage';
import { ProtectedRoute } from '@/routes/ProtectedRoute';
import { RoleRoute } from '@/routes/RoleRoute';
import { AppShell } from '@/components/layout/AppShell';

// Phase 2 Patient Views
import { PatientDashboardPage } from '@/features/patient/PatientDashboardPage';
import { PatientAppointmentsPage } from '@/features/patient/PatientAppointmentsPage';
import { DoctorDirectoryPage } from '@/features/doctors/DoctorDirectoryPage';
import { PatientLiveQueuePage } from '@/features/queue/PatientLiveQueuePage';
import { PatientMedicalHistoryPage } from '@/features/medical-history/PatientMedicalHistoryPage';

// Phase 2 Doctor Views
import { DoctorDashboardPage } from '@/features/doctor/DoctorDashboardPage';
import { DoctorQueueConsolePage } from '@/features/doctor/DoctorQueueConsolePage';
import { DoctorConsultationWorkspacePage } from '@/features/doctor/DoctorConsultationWorkspacePage';
import { DoctorSchedulePage } from '@/features/doctor/DoctorSchedulePage';

// Phase 3 Admin Views
import { AdminDashboardOverviewPage } from '@/features/admin/AdminDashboardOverviewPage';
import { AdminUserDirectoryPage } from '@/features/admin/AdminUserDirectoryPage';
import { AdminDoctorsPage } from '@/features/admin/AdminDoctorsPage';
import { AdminDepartmentsPage } from '@/features/admin/AdminDepartmentsPage';
import { AdminQueueMonitorPage } from '@/features/admin/AdminQueueMonitorPage';
import { AdminSchedulesPage } from '@/features/admin/AdminSchedulesPage';
import { AdminAnalyticsPage } from '@/features/admin/AdminAnalyticsPage';
import { AdminAuditLogsPage } from '@/features/admin/AdminAuditLogsPage';

import { useAuth } from '@/lib/auth/AuthContext';

// Helper component to route /dashboard to the user's role-specific space
const RoleRedirect: React.FC = () => {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  switch (user.role) {
    case 'ADMIN':
      return <Navigate to="/admin" replace />;
    case 'DOCTOR':
      return <Navigate to="/doctor" replace />;
    case 'PATIENT':
    default:
      return <Navigate to="/patient" replace />;
  }
};

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/unauthorized" element={<UnauthorizedPage />} />

      {/* Protected Routes */}
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<RoleRedirect />} />

        {/* Patient Portal Routes */}
        <Route element={<RoleRoute allowedRoles={['PATIENT']} />}>
          <Route
            path="/patient"
            element={
              <AppShell>
                <PatientDashboardPage />
              </AppShell>
            }
          />
          <Route
            path="/patient/appointments"
            element={
              <AppShell>
                <PatientAppointmentsPage />
              </AppShell>
            }
          />
          <Route
            path="/patient/doctors"
            element={
              <AppShell>
                <DoctorDirectoryPage />
              </AppShell>
            }
          />
          <Route
            path="/patient/queue"
            element={
              <AppShell>
                <PatientLiveQueuePage />
              </AppShell>
            }
          />
          <Route
            path="/patient/records"
            element={
              <AppShell>
                <PatientMedicalHistoryPage />
              </AppShell>
            }
          />
        </Route>

        {/* Doctor Workspace Routes */}
        <Route element={<RoleRoute allowedRoles={['DOCTOR']} />}>
          <Route
            path="/doctor"
            element={
              <AppShell>
                <DoctorDashboardPage />
              </AppShell>
            }
          />
          <Route
            path="/doctor/queue"
            element={
              <AppShell>
                <DoctorQueueConsolePage />
              </AppShell>
            }
          />
          <Route
            path="/doctor/consultations"
            element={
              <AppShell>
                <DoctorConsultationWorkspacePage />
              </AppShell>
            }
          />
          <Route
            path="/doctor/schedule"
            element={
              <AppShell>
                <DoctorSchedulePage />
              </AppShell>
            }
          />
        </Route>

        {/* Administrator Workspace (Phase 3 Complete) */}
        <Route element={<RoleRoute allowedRoles={['ADMIN']} />}>
          <Route
            path="/admin"
            element={
              <AppShell>
                <AdminDashboardOverviewPage />
              </AppShell>
            }
          />
          <Route
            path="/admin/users"
            element={
              <AppShell>
                <AdminUserDirectoryPage />
              </AppShell>
            }
          />
          <Route
            path="/admin/doctors"
            element={
              <AppShell>
                <AdminDoctorsPage />
              </AppShell>
            }
          />
          <Route
            path="/admin/departments"
            element={
              <AppShell>
                <AdminDepartmentsPage />
              </AppShell>
            }
          />
          <Route
            path="/admin/queues"
            element={
              <AppShell>
                <AdminQueueMonitorPage />
              </AppShell>
            }
          />
          <Route
            path="/admin/schedules"
            element={
              <AppShell>
                <AdminSchedulesPage />
              </AppShell>
            }
          />
          <Route
            path="/admin/analytics"
            element={
              <AppShell>
                <AdminAnalyticsPage />
              </AppShell>
            }
          />
          <Route
            path="/admin/audit"
            element={
              <AppShell>
                <AdminAuditLogsPage />
              </AppShell>
            }
          />
        </Route>
      </Route>

      {/* 404 Catch-All */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
};
