import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { queueService } from '@/lib/api/queueService';
import { useQueueSubscription } from '@/lib/websocket/useQueueSubscription';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { EmptyState } from '@/components/ui/EmptyState';
import {
  Clock,
  Activity,
  Users,
  RefreshCw,
  Bell,
} from 'lucide-react';

export const PatientLiveQueuePage: React.FC = () => {
  const navigate = useNavigate();
  // STOMP WebSocket real-time subscription for patient queue
  const { isConnected, isConnecting } = useQueueSubscription({
    isPatient: true,
  });

  // Active queue entry query
  const {
    data: activeEntry,
    isLoading: isEntryLoading,
    isError: isEntryError,
    refetch: refetchEntry,
  } = useQuery({
    queryKey: ['queue', 'my-status'],
    queryFn: () => queueService.getMyActiveQueueStatus(),
    refetchInterval: isConnected ? false : 15000, // Fallback polling every 15s if WS disconnected
  });

  // Wait time prediction query
  const { data: waitEstimate, refetch: refetchEstimate } = useQuery({
    queryKey: ['queue', 'my-wait-estimate', activeEntry?.id],
    queryFn: () => queueService.getMyWaitTimeEstimate(),
    enabled: Boolean(activeEntry?.id),
    refetchInterval: isConnected ? false : 30000,
  });

  // Queue audit events query
  const { data: events } = useQuery({
    queryKey: ['queue', 'events', activeEntry?.id],
    queryFn: () => queueService.getQueueEvents(activeEntry!.id),
    enabled: Boolean(activeEntry?.id),
  });

  const handleManualRefresh = () => {
    refetchEntry();
    refetchEstimate();
  };

  const getStatusBadge = (status?: string) => {
    switch (status) {
      case 'WAITING':
        return <Badge variant="warning">Waiting in Line</Badge>;
      case 'CALLED':
        return (
          <Badge variant="primary" className="animate-pulse">
            Called — Please Enter Room
          </Badge>
        );
      case 'IN_PROGRESS':
        return <Badge variant="success">In Consultation</Badge>;
      case 'SKIPPED':
        return <Badge variant="error">Skipped (Absent)</Badge>;
      case 'COMPLETED':
        return <Badge variant="success">Completed</Badge>;
      default:
        return <Badge variant="neutral">{status ?? 'UNKNOWN'}</Badge>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-text-primary tracking-tight font-heading">
              Live Digital Queue Tracker
            </h1>
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border border-surface-border bg-surface-elevated">
              {isConnected ? (
                <>
                  <span className="w-2 h-2 rounded-full bg-status-success animate-pulse" />
                  <span className="text-status-success">Real-Time Connected</span>
                </>
              ) : isConnecting ? (
                <>
                  <span className="w-2 h-2 rounded-full bg-status-warning" />
                  <span className="text-status-warning">Reconnecting WS...</span>
                </>
              ) : (
                <>
                  <span className="w-2 h-2 rounded-full bg-text-muted" />
                  <span className="text-text-muted">Auto-Refresh Mode</span>
                </>
              )}
            </div>
          </div>
          <p className="text-sm text-text-secondary mt-1">
            Real-time algorithmic queue position and estimated waiting time for today's appointment.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleManualRefresh}
            leftIcon={<RefreshCw className="w-4 h-4" />}
          >
            Refresh Snapshot
          </Button>
        </div>
      </div>

      {/* Main Content */}
      {isEntryLoading ? (
        <div className="p-16 flex flex-col items-center justify-center gap-3">
          <Spinner size="lg" className="text-brand-lavender" />
          <p className="text-sm font-medium text-text-secondary">Checking today's queue status...</p>
        </div>
      ) : isEntryError || !activeEntry ? (
        <EmptyState
          title="No Active Queue Token Today"
          description="You do not have an active queue token in progress today. To receive a sequential digital token, check in on the day of your booked appointment."
          actionLabel="View My Appointments"
          onAction={() => navigate('/patient/appointments')}
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Token Hero Display (2 Columns) */}
          <div className="lg:col-span-2 space-y-6">
            <Card className="border-surface-border shadow-soft-md overflow-hidden">
              <div className="bg-brand-deep text-white px-6 py-4 flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <Activity className="w-5 h-5 text-brand-lavender" />
                  <span className="font-semibold text-sm">Active Hospital Queue Token</span>
                </div>
                <span className="text-xs text-white/70">
                  Ref: #{activeEntry.entryReference}
                </span>
              </div>

              <CardContent className="p-6 space-y-6">
                {/* Hero Token Number */}
                <div className="p-6 rounded-2xl bg-surface-lavender/40 border border-surface-border text-center space-y-2">
                  <span className="text-xs uppercase tracking-wider font-semibold text-text-secondary">
                    Your Sequential Token
                  </span>
                  <div className="text-5xl sm:text-6xl font-extrabold text-brand-deep dark:text-brand-lavender font-heading tracking-tight">
                    #{activeEntry.tokenNumber}
                  </div>
                  <div className="pt-1">{getStatusBadge(activeEntry.status)}</div>
                </div>

                {/* Called Alert Callout if CALLED */}
                {activeEntry.status === 'CALLED' && (
                  <div className="p-4 rounded-xl bg-brand-lavender/15 border border-brand-lavender/40 text-brand-deep dark:text-brand-lavender flex items-start gap-3 animate-pulse">
                    <Bell className="w-5 h-5 shrink-0 mt-0.5" />
                    <div>
                      <h2 className="font-bold text-sm">Your Token Has Been Called!</h2>
                      <p className="text-xs mt-0.5 leading-relaxed">
                        Please proceed to the consultation room for{' '}
                        <strong>{activeEntry.doctorName}</strong> in{' '}
                        <strong>{activeEntry.departmentName}</strong>.
                      </p>
                    </div>
                  </div>
                )}

                {/* Details Breakdown */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                  <div className="p-4 rounded-xl bg-surface-elevated border border-surface-border space-y-1">
                    <span className="text-xs text-text-secondary font-medium">Attending Clinician</span>
                    <p className="font-bold text-text-primary text-base">{activeEntry.doctorName}</p>
                    <p className="text-xs text-brand-lavender font-medium">{activeEntry.departmentName}</p>
                  </div>

                  <div className="p-4 rounded-xl bg-surface-elevated border border-surface-border space-y-1">
                    <span className="text-xs text-text-secondary font-medium">Check-In Timestamp</span>
                    <p className="font-semibold text-text-primary">
                      {new Date(activeEntry.checkInTimestamp).toLocaleTimeString([], {
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </p>
                    <p className="text-xs text-text-muted">
                      Priority Category: {activeEntry.priorityCategory}
                    </p>
                  </div>
                </div>

                {/* Predictive Waiting Metrics */}
                {waitEstimate && (
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 pt-2 border-t border-surface-border">
                    <div className="p-3.5 rounded-xl bg-surface-lavender/30 border border-surface-border text-center">
                      <div className="flex items-center justify-center gap-1 text-xs text-text-secondary mb-1">
                        <Clock className="w-3.5 h-3.5 text-brand-lavender" />
                        <span>Estimated Wait</span>
                      </div>
                      <span className="text-xl font-extrabold text-brand-deep dark:text-surface-card">
                        ~{waitEstimate.estimatedWaitMinutes} mins
                      </span>
                      {waitEstimate.lowerBoundMinutes !== undefined &&
                        waitEstimate.upperBoundMinutes !== undefined && (
                          <p className="text-[10px] text-text-muted mt-0.5">
                            Range: {waitEstimate.lowerBoundMinutes}–{waitEstimate.upperBoundMinutes} mins
                          </p>
                        )}
                    </div>

                    <div className="p-3.5 rounded-xl bg-surface-lavender/30 border border-surface-border text-center">
                      <div className="flex items-center justify-center gap-1 text-xs text-text-secondary mb-1">
                        <Users className="w-3.5 h-3.5 text-brand-lavender" />
                        <span>Patients Ahead</span>
                      </div>
                      <span className="text-xl font-extrabold text-brand-deep dark:text-surface-card">
                        {waitEstimate.eligiblePatientsAhead}
                      </span>
                      <p className="text-[10px] text-text-muted mt-0.5">Eligible ahead in line</p>
                    </div>

                    <div className="col-span-2 sm:col-span-1 p-3.5 rounded-xl bg-surface-lavender/30 border border-surface-border text-center flex flex-col justify-center">
                      <div className="text-xs text-text-secondary mb-1">Prediction Mode</div>
                      <span className="text-xs font-bold text-text-primary">
                        {waitEstimate.predictionMethod.replace('_', ' ')}
                      </span>
                      <span className="text-[10px] text-text-muted mt-0.5">
                        {waitEstimate.queuePaused ? 'Queue is paused' : 'Active flow'}
                      </span>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Audit Events Timeline (1 Column) */}
          <div className="space-y-4">
            <h2 className="text-lg font-bold text-text-primary tracking-tight font-heading">
              Queue Lifecycle Audit
            </h2>

            <Card className="border-surface-border">
              <CardContent className="p-5">
                {!events || events.length === 0 ? (
                  <p className="text-xs text-text-muted text-center py-4">No audit events recorded.</p>
                ) : (
                  <div className="relative pl-6 space-y-4 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-surface-border">
                    {events.map((evt, idx) => (
                      <div key={idx} className="relative">
                        <div className="absolute -left-6 top-1 w-2.5 h-2.5 rounded-full bg-brand-lavender ring-4 ring-surface-card" />
                        <div>
                          <p className="text-xs font-bold text-text-primary">{evt.eventType}</p>
                          <div className="flex items-center gap-1.5 text-[11px] text-text-secondary mt-0.5">
                            <span>Status: {evt.newStatus}</span>
                            <span>•</span>
                            <span>{new Date(evt.eventTimestamp).toLocaleTimeString()}</span>
                          </div>
                          {evt.details && (
                            <p className="text-[11px] text-text-muted mt-0.5 italic">{evt.details}</p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
};
