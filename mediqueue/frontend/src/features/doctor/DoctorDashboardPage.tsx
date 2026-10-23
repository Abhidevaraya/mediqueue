import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/lib/feedback/ToastContext';
import { appointmentService } from '@/lib/api/appointmentService';
import { queueService } from '@/lib/api/queueService';
import { useQueueSubscription } from '@/lib/websocket/useQueueSubscription';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { Dialog } from '@/components/ui/Dialog';
import {
  Clock,
  Play,
  Pause,
  PhoneCall,
  ArrowRight,
} from 'lucide-react';

export const DoctorDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  // Real-time STOMP subscription for doctor's queue
  const { isConnected } = useQueueSubscription({
    doctorId: user?.id,
  });

  const [isPauseDialogOpen, setIsPauseDialogOpen] = useState(false);
  const [pauseReason, setPauseReason] = useState('');

  // Today's appointments query
  const { data: todayAppointments, isLoading: isApptsLoading } = useQuery({
    queryKey: ['appointments', 'doctor', 'today'],
    queryFn: () => appointmentService.getDoctorTodayAppointments(),
  });

  // Today's queue summary query
  const { data: queueSummary } = useQuery({
    queryKey: ['queue', 'doctor', 'today', user?.id],
    queryFn: () => queueService.getDoctorQueueToday(user!.id),
    enabled: Boolean(user?.id),
    refetchInterval: isConnected ? false : 15000,
  });

  // Call next patient mutation
  const callNextMutation = useMutation({
    mutationFn: () => queueService.callNext(),
    onSuccess: (entry) => {
      toast.success(
        `Called next patient: #${entry.tokenNumber} (${entry.patientName})`,
        'Patient Called'
      );
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'No eligible waiting patients to call');
    },
  });

  // Pause queue mutation
  const pauseMutation = useMutation({
    mutationFn: (reason?: string) => queueService.pauseQueue(reason),
    onSuccess: () => {
      toast.info('Queue paused. Waiting patients have been notified.', 'Queue Paused');
      queryClient.invalidateQueries({ queryKey: ['queue'] });
      setIsPauseDialogOpen(false);
      setPauseReason('');
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to pause queue');
    },
  });

  // Resume queue mutation
  const resumeMutation = useMutation({
    mutationFn: () => queueService.resumeQueue(),
    onSuccess: () => {
      toast.success('Queue resumed successfully', 'Queue Resumed');
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to resume queue');
    },
  });

  const appointments = todayAppointments || [];

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-text-primary tracking-tight font-heading">
              Clinical Workspace
            </h1>
            <Badge variant="primary" size="md">
              Dr. {user?.fullName}
            </Badge>
          </div>
          <p className="text-sm text-text-secondary mt-1">
            Manage your daily consultations, OPD queue status, and call waiting patients.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {queueSummary?.isPaused ? (
            <Button
              variant="primary"
              size="sm"
              isLoading={resumeMutation.isPending}
              onClick={() => resumeMutation.mutate()}
              leftIcon={<Play className="w-4 h-4" />}
            >
              Resume Queue
            </Button>
          ) : (
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsPauseDialogOpen(true)}
              leftIcon={<Pause className="w-4 h-4" />}
            >
              Pause Queue
            </Button>
          )}

          <Link to="/doctor/queue">
            <Button variant="secondary" size="sm" rightIcon={<ArrowRight className="w-4 h-4" />}>
              OPD Queue Console
            </Button>
          </Link>
        </div>
      </div>

      {/* OPD Queue Metric Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <Card className="border-surface-border">
          <CardContent className="p-4 text-center">
            <span className="text-xs font-semibold text-text-muted">Total Checked In</span>
            <p className="text-2xl font-extrabold text-text-primary mt-1">
              {queueSummary?.totalCheckedIn ?? 0}
            </p>
          </CardContent>
        </Card>

        <Card className="border-surface-border bg-status-warning-surface/30">
          <CardContent className="p-4 text-center">
            <span className="text-xs font-semibold text-status-warning">Waiting</span>
            <p className="text-2xl font-extrabold text-status-warning mt-1">
              {queueSummary?.waitingCount ?? 0}
            </p>
          </CardContent>
        </Card>

        <Card className="border-surface-border bg-brand-surface/40">
          <CardContent className="p-4 text-center">
            <span className="text-xs font-semibold text-brand-lavender">Called</span>
            <p className="text-2xl font-extrabold text-brand-lavender mt-1">
              {queueSummary?.calledCount ?? 0}
            </p>
          </CardContent>
        </Card>

        <Card className="border-surface-border bg-status-success-surface/30">
          <CardContent className="p-4 text-center">
            <span className="text-xs font-semibold text-status-success">In Consultation</span>
            <p className="text-2xl font-extrabold text-status-success mt-1">
              {queueSummary?.inProgressCount ?? 0}
            </p>
          </CardContent>
        </Card>

        <Card className="border-surface-border">
          <CardContent className="p-4 text-center">
            <span className="text-xs font-semibold text-text-muted">Completed</span>
            <p className="text-2xl font-extrabold text-text-primary mt-1">
              {queueSummary?.completedCount ?? 0}
            </p>
          </CardContent>
        </Card>

        <Card className="border-surface-border">
          <CardContent className="p-4 text-center">
            <span className="text-xs font-semibold text-text-muted">Skipped</span>
            <p className="text-2xl font-extrabold text-text-primary mt-1">
              {queueSummary?.skippedCount ?? 0}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Hero Call Patient Console */}
      <Card className="border-brand-lavender/30 bg-gradient-to-r from-brand-surface/40 to-surface-card shadow-soft-md">
        <CardContent className="p-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
            <div className="space-y-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-brand-lavender">
                OPD Station Quick Action
              </span>
              <div className="flex items-baseline gap-3">
                <h2 className="text-3xl font-extrabold text-text-primary font-heading">
                  Currently Serving:{' '}
                  {queueSummary?.currentCalledToken
                    ? `#${queueSummary.currentCalledToken}`
                    : queueSummary?.currentServingToken
                    ? `#${queueSummary.currentServingToken}`
                    : 'None Called'}
                </h2>
              </div>
              <p className="text-xs text-text-secondary">
                {queueSummary?.waitingCount ?? 0} patients currently waiting in line for consultation.
              </p>
            </div>

            <div className="flex items-center gap-3 shrink-0">
              <Button
                variant="primary"
                size="lg"
                isLoading={callNextMutation.isPending}
                disabled={(queueSummary?.waitingCount ?? 0) === 0}
                onClick={() => callNextMutation.mutate()}
                leftIcon={<PhoneCall className="w-5 h-5" />}
              >
                Call Next Patient
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Today's Appointments List */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-text-primary tracking-tight font-heading">
            Today's Scheduled Consultations ({appointments.length})
          </h2>
          <Link to="/doctor/consultations" className="text-xs font-semibold text-brand-lavender hover:underline">
            View Consultation Workspace
          </Link>
        </div>

        {isApptsLoading ? (
          <div className="p-12 text-center">
            <Spinner size="md" className="text-brand-lavender mx-auto mb-2" />
            <p className="text-xs text-text-secondary">Loading today's schedule...</p>
          </div>
        ) : appointments.length === 0 ? (
          <Card className="border-surface-border">
            <CardContent className="p-8 text-center text-xs text-text-muted">
              No appointments booked for today.
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {appointments.map((appt) => {
              const time = new Date(appt.startDateTime).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
              });

              return (
                <Card key={appt.id} className="border-surface-border hover:shadow-soft-sm transition-shadow">
                  <CardContent className="p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-brand-lavender flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5" />
                        {time}
                      </span>
                      <Badge variant="primary" size="sm">
                        {appt.status}
                      </Badge>
                    </div>

                    <div>
                      <h3 className="font-bold text-text-primary text-sm truncate">{appt.patientName}</h3>
                      <p className="text-xs text-text-secondary">Phone: {appt.patientPhone || 'Not provided'}</p>
                      {appt.reason && (
                        <p className="text-[11px] text-text-muted mt-1 italic line-clamp-1">
                          Reason: {appt.reason}
                        </p>
                      )}
                    </div>

                    <div className="pt-2 border-t border-surface-border flex items-center justify-between">
                      <span className="text-[10px] text-text-muted">Ref #{appt.appointmentReference}</span>
                      <Link to="/doctor/consultations">
                        <Button variant="ghost" size="sm" className="h-8 text-xs">
                          Open File
                        </Button>
                      </Link>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}
      </div>

      {/* Pause Queue Dialog */}
      <Dialog
        isOpen={isPauseDialogOpen}
        onClose={() => setIsPauseDialogOpen(false)}
        title="Pause Doctor Queue"
      >
        <div className="space-y-4">
          <p className="text-xs text-text-secondary">
            Pausing the queue prevents calling new patients and notifies waiting patients of a temporary
            delay.
          </p>

          <div>
            <label htmlFor="pause-reason" className="block text-xs font-semibold text-text-primary mb-1">
              Pause Reason (Optional)
            </label>
            <input
              id="pause-reason"
              type="text"
              placeholder="e.g. In emergency surgery, 15 min break..."
              value={pauseReason}
              onChange={(e) => setPauseReason(e.target.value)}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-surface-border">
            <Button variant="ghost" size="sm" onClick={() => setIsPauseDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={pauseMutation.isPending}
              onClick={() => pauseMutation.mutate(pauseReason || undefined)}
            >
              Confirm Pause
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
};
