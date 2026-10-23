import React, { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { adminService } from '@/lib/api/adminService';
import { HospitalOverviewResponse, AdminQueueSummaryResponse, QueueSessionResponse } from '@/types/admin';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import {
  Calendar,
  Users,
  Clock,
  Activity,
  CheckCircle2,
  Hourglass,
  Layers,
  Pause,
  Play,
  ArrowRight,
  TrendingUp,
  ShieldAlert,
} from 'lucide-react';

export const AdminDashboardOverviewPage: React.FC = () => {
  const [overview, setOverview] = useState<HospitalOverviewResponse | null>(null);
  const [queues, setQueues] = useState<AdminQueueSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Queue pause/resume confirmation dialog state
  const [selectedQueue, setSelectedQueue] = useState<AdminQueueSummaryResponse | null>(null);
  const [actionType, setActionType] = useState<'pause' | 'resume' | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const fetchDashboardData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [overviewData, queueData] = await Promise.all([
        adminService.getOverview(),
        adminService.getTodayQueues(),
      ]);
      setOverview(overviewData);
      setQueues(queueData);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load hospital operational data';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const handleConfirmQueueAction = async () => {
    if (!selectedQueue || !actionType) return;
    setSubmitting(true);
    setActionError(null);
    try {
      let updated: QueueSessionResponse;
      if (actionType === 'pause') {
        updated = await adminService.pauseQueue(selectedQueue.sessionId);
      } else {
        updated = await adminService.resumeQueue(selectedQueue.sessionId);
      }
      setQueues((prev) =>
        prev.map((q) =>
          q.sessionId === updated.id ? { ...q, isPaused: updated.isPaused } : q
        )
      );
      setSelectedQueue(null);
      setActionType(null);
      fetchDashboardData().catch(() => {});
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to update queue session status';
      setActionError(message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[50vh] gap-3">
        <Spinner size="lg" />
        <p className="text-sm font-medium text-text-secondary">Loading hospital overview metrics...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto py-8">
        <ErrorState
          title="Hospital Overview Unavailable"
          message={error}
          onRetry={fetchDashboardData}
        />
      </div>
    );
  }

  const currentDateStr = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-brand-lavender/10 text-brand-lavender">
              Live Operations
            </span>
            <span className="text-xs text-text-muted">{currentDateStr}</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-text-primary tracking-tight mt-1">
            Hospital Administration Overview
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Executive OPD command center, real-time wait-time analytics, and clinic supervision.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button variant="secondary" onClick={fetchDashboardData} size="sm">
            Refresh Metrics
          </Button>
          <Link to="/admin/analytics">
            <Button variant="primary" size="sm">
              <TrendingUp className="w-4 h-4 mr-1.5" />
              Full Analytics
            </Button>
          </Link>
        </div>
      </div>

      {/* Metric Cards Grid */}
      {overview && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          <Card className="p-4 border-l-4 border-l-brand-lavender">
            <div className="flex items-center justify-between text-text-muted">
              <span className="text-xs font-semibold uppercase tracking-wider">Today's Bookings</span>
              <Calendar className="w-4 h-4 text-brand-lavender" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-text-primary">
                {overview.totalAppointmentsToday}
              </span>
            </div>
            <p className="text-xs text-text-muted mt-1">All OPD appointments</p>
          </Card>

          <Card className="p-4 border-l-4 border-l-state-success">
            <div className="flex items-center justify-between text-text-muted">
              <span className="text-xs font-semibold uppercase tracking-wider">Completed</span>
              <CheckCircle2 className="w-4 h-4 text-state-success" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-text-primary">
                {overview.completedAppointmentsToday}
              </span>
            </div>
            <p className="text-xs text-text-muted mt-1">Finished consultations</p>
          </Card>

          <Card className="p-4 border-l-4 border-l-brand-indigo">
            <div className="flex items-center justify-between text-text-muted">
              <span className="text-xs font-semibold uppercase tracking-wider">Active Queues</span>
              <Activity className="w-4 h-4 text-brand-indigo" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-text-primary">
                {overview.activeQueueSessionsToday}
              </span>
            </div>
            <p className="text-xs text-text-muted mt-1">Active doctor clinics</p>
          </Card>

          <Card className="p-4 border-l-4 border-l-state-warning">
            <div className="flex items-center justify-between text-text-muted">
              <span className="text-xs font-semibold uppercase tracking-wider">In Waiting</span>
              <Hourglass className="w-4 h-4 text-state-warning" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-text-primary">
                {overview.waitingPatientsToday}
              </span>
            </div>
            <p className="text-xs text-text-muted mt-1">Patients checked-in</p>
          </Card>

          <Card className="p-4 border-l-4 border-l-brand-lavender">
            <div className="flex items-center justify-between text-text-muted">
              <span className="text-xs font-semibold uppercase tracking-wider">Avg Wait Time</span>
              <Clock className="w-4 h-4 text-brand-lavender" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-text-primary">
                {overview.averageWaitTimeMinutesToday}
              </span>
              <span className="text-xs text-text-secondary font-medium">mins</span>
            </div>
            <p className="text-xs text-text-muted mt-1">Door-to-consultation</p>
          </Card>

          <Card className="p-4 border-l-4 border-l-brand-indigo">
            <div className="flex items-center justify-between text-text-muted">
              <span className="text-xs font-semibold uppercase tracking-wider">Avg Consult</span>
              <Activity className="w-4 h-4 text-brand-indigo" />
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold text-text-primary">
                {overview.averageConsultationDurationMinutesToday}
              </span>
              <span className="text-xs text-text-secondary font-medium">mins</span>
            </div>
            <p className="text-xs text-text-muted mt-1">Per patient encounter</p>
          </Card>
        </div>
      )}

      {/* Operational Modules Navigation Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Link to="/admin/users" className="group">
          <Card className="p-5 h-full transition-all duration-200 hover:shadow-soft-md hover:border-brand-lavender/50">
            <div className="w-10 h-10 rounded-xl bg-brand-lavender/10 text-brand-lavender flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
              <Users className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-text-primary group-hover:text-brand-lavender transition-colors">
              User Directory
            </h3>
            <p className="text-xs text-text-secondary mt-1">
              Search hospital accounts, manage roles, block/activate users, and audit profiles.
            </p>
            <div className="mt-3 flex items-center text-xs font-semibold text-brand-lavender">
              <span>View Directory</span>
              <ArrowRight className="w-3.5 h-3.5 ml-1 group-hover:translate-x-1 transition-transform" />
            </div>
          </Card>
        </Link>

        <Link to="/admin/doctors" className="group">
          <Card className="p-5 h-full transition-all duration-200 hover:shadow-soft-md hover:border-brand-lavender/50">
            <div className="w-10 h-10 rounded-xl bg-brand-indigo/10 text-brand-indigo flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
              <Activity className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-text-primary group-hover:text-brand-lavender transition-colors">
              Doctor Management
            </h3>
            <p className="text-xs text-text-secondary mt-1">
              Onboard practitioners, verify credentials, edit medical specialties and status.
            </p>
            <div className="mt-3 flex items-center text-xs font-semibold text-brand-lavender">
              <span>Manage Doctors</span>
              <ArrowRight className="w-3.5 h-3.5 ml-1 group-hover:translate-x-1 transition-transform" />
            </div>
          </Card>
        </Link>

        <Link to="/admin/departments" className="group">
          <Card className="p-5 h-full transition-all duration-200 hover:shadow-soft-md hover:border-brand-lavender/50">
            <div className="w-10 h-10 rounded-xl bg-state-success/10 text-state-success flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
              <Layers className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-text-primary group-hover:text-brand-lavender transition-colors">
              Department Catalog
            </h3>
            <p className="text-xs text-text-secondary mt-1">
              Configure hospital clinics, departments, active statuses, and service descriptions.
            </p>
            <div className="mt-3 flex items-center text-xs font-semibold text-brand-lavender">
              <span>Configure Departments</span>
              <ArrowRight className="w-3.5 h-3.5 ml-1 group-hover:translate-x-1 transition-transform" />
            </div>
          </Card>
        </Link>

        <Link to="/admin/audit" className="group">
          <Card className="p-5 h-full transition-all duration-200 hover:shadow-soft-md hover:border-brand-lavender/50">
            <div className="w-10 h-10 rounded-xl bg-brand-lavender/10 text-brand-lavender flex items-center justify-center mb-3 group-hover:scale-105 transition-transform">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <h3 className="font-semibold text-text-primary group-hover:text-brand-lavender transition-colors">
              Clinical Audit Trail
            </h3>
            <p className="text-xs text-text-secondary mt-1">
              Examine security access, consultation events, staff actions, and export CSV logs.
            </p>
            <div className="mt-3 flex items-center text-xs font-semibold text-brand-lavender">
              <span>View Audit Logs</span>
              <ArrowRight className="w-3.5 h-3.5 ml-1 group-hover:translate-x-1 transition-transform" />
            </div>
          </Card>
        </Link>
      </div>

      {/* Today's Queue Supervision Table */}
      <Card className="overflow-hidden">
        <div className="p-5 border-b border-surface-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h2 className="text-lg font-bold text-text-primary">Today's OPD Queue Sessions</h2>
            <p className="text-xs text-text-secondary mt-0.5">
              Supervise active room sessions across departments and issue pause/resume overrides.
            </p>
          </div>
          <Link to="/admin/queues">
            <Button variant="secondary" size="sm">
              <Clock className="w-4 h-4 mr-1.5" />
              Full Queue Console
            </Button>
          </Link>
        </div>

        {queues.length === 0 ? (
          <div className="p-8 text-center text-text-muted">
            <Clock className="w-10 h-10 mx-auto text-text-muted/40 mb-2" />
            <p className="font-medium text-sm">No active queue sessions registered for today yet.</p>
            <p className="text-xs mt-1">Sessions appear automatically once doctors check in or open clinic sessions.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-surface-border bg-surface-subtle/50 text-xs font-semibold text-text-muted uppercase tracking-wider">
                  <th className="px-5 py-3">Session</th>
                  <th className="px-5 py-3">Doctor & Clinic</th>
                  <th className="px-5 py-3">Department</th>
                  <th className="px-5 py-3">Current Token</th>
                  <th className="px-5 py-3">Waiting</th>
                  <th className="px-5 py-3">Total Registered</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-border">
                {queues.map((session) => (
                  <tr key={session.sessionId} className="hover:bg-surface-subtle/40 transition-colors">
                    <td className="px-5 py-3.5 font-mono text-xs text-text-muted">
                      #{session.sessionId}
                    </td>
                    <td className="px-5 py-3.5 font-medium text-text-primary">
                      {session.doctorName}
                    </td>
                    <td className="px-5 py-3.5 text-text-secondary">
                      {session.departmentName}
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="font-mono font-bold text-brand-lavender text-sm">
                        {session.currentInProgressToken ? `#${session.currentInProgressToken}` : (session.currentCalledToken ? `Called #${session.currentCalledToken}` : '—')}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge variant="warning">{session.waitingCount} waiting</Badge>
                    </td>
                    <td className="px-5 py-3.5 text-text-secondary">
                      {session.totalPatients} patients
                    </td>
                    <td className="px-5 py-3.5">
                      {session.isPaused ? (
                        <Badge variant="neutral">Paused</Badge>
                      ) : (
                        <Badge variant="success">Active</Badge>
                      )}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      {session.isPaused ? (
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => {
                            setSelectedQueue(session);
                            setActionType('resume');
                          }}
                        >
                          <Play className="w-3.5 h-3.5 mr-1 text-state-success" />
                          Resume
                        </Button>
                      ) : (
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => {
                            setSelectedQueue(session);
                            setActionType('pause');
                          }}
                        >
                          <Pause className="w-3.5 h-3.5 mr-1 text-state-warning" />
                          Pause
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Confirmation Dialog for Pause/Resume */}
      <ConfirmDialog
        isOpen={Boolean(selectedQueue && actionType)}
        title={actionType === 'pause' ? 'Pause Queue Session?' : 'Resume Queue Session?'}
        message={
          actionType === 'pause'
            ? `Are you sure you want to pause Queue Session #${selectedQueue?.sessionId} (${selectedQueue?.doctorName})? Patients will see that OPD calling is temporarily suspended.`
            : `Are you sure you want to resume Queue Session #${selectedQueue?.sessionId} (${selectedQueue?.doctorName})? OPD token calling will reactivate immediately.`
        }
        confirmText={actionType === 'pause' ? 'Confirm Pause' : 'Confirm Resume'}
        variant={actionType === 'pause' ? 'danger' : 'primary'}
        isLoading={submitting}
        onConfirm={handleConfirmQueueAction}
        onClose={() => {
          setSelectedQueue(null);
          setActionType(null);
          setActionError(null);
        }}
      />

      {actionError && (
        <div className="p-4 rounded-xl bg-state-error/10 border border-state-error/20 text-state-error text-sm">
          {actionError}
        </div>
      )}
    </div>
  );
};
