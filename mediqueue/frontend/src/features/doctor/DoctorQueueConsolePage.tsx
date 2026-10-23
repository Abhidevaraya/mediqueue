import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/lib/feedback/ToastContext';
import { queueService } from '@/lib/api/queueService';
import { useQueueSubscription } from '@/lib/websocket/useQueueSubscription';
import { QueueEntryResponse } from '@/types/queue';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { Dialog } from '@/components/ui/Dialog';
import {
  PhoneCall,
  Play,
  CheckCircle2,
  RotateCcw,
  Activity,
  RefreshCw,
} from 'lucide-react';

export const DoctorQueueConsolePage: React.FC = () => {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  const { isConnected } = useQueueSubscription({
    doctorId: user?.id,
  });

  const [skippingEntry, setSkippingEntry] = useState<QueueEntryResponse | null>(null);
  const [skipReason, setSkipReason] = useState<string>('');

  // Queue summary query
  const {
    data: queueSummary,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['queue', 'doctor', 'today', user?.id],
    queryFn: () => queueService.getDoctorQueueToday(user!.id),
    enabled: Boolean(user?.id),
    refetchInterval: isConnected ? false : 10000,
  });

  // Action mutations
  const callEntryMutation = useMutation({
    mutationFn: (id: number) => queueService.callEntry(id),
    onSuccess: (entry) => {
      toast.success(`Called token #${entry.tokenNumber} (${entry.patientName})`, 'Patient Called');
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
    onError: (err: any) => toast.error(err?.message || 'Failed to call token'),
  });

  const recallEntryMutation = useMutation({
    mutationFn: (id: number) => queueService.recallEntry(id),
    onSuccess: (entry) => {
      toast.info(`Recalled token #${entry.tokenNumber}`, 'Notification Resent');
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
    onError: (err: any) => toast.error(err?.message || 'Failed to recall patient'),
  });

  const startConsultMutation = useMutation({
    mutationFn: (id: number) => queueService.startConsultation(id),
    onSuccess: (entry) => {
      toast.success(`Consultation started for token #${entry.tokenNumber}`, 'Session In Progress');
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
    onError: (err: any) => toast.error(err?.message || 'Failed to start consultation'),
  });

  const completeConsultMutation = useMutation({
    mutationFn: (id: number) => queueService.completeConsultation(id),
    onSuccess: (entry) => {
      toast.success(
        `Consultation completed for token #${entry.tokenNumber}`,
        'Consultation Finished'
      );
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
    onError: (err: any) => toast.error(err?.message || 'Failed to complete consultation'),
  });

  const skipEntryMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      queueService.skipEntry(id, reason),
    onSuccess: (entry) => {
      toast.warning(`Token #${entry.tokenNumber} marked as SKIPPED`, 'Patient Skipped');
      queryClient.invalidateQueries({ queryKey: ['queue'] });
      setSkippingEntry(null);
      setSkipReason('');
    },
    onError: (err: any) => toast.error(err?.message || 'Failed to skip patient'),
  });

  const requeueMutation = useMutation({
    mutationFn: (id: number) => queueService.requeueEntry(id),
    onSuccess: (entry) => {
      toast.success(`Token #${entry.tokenNumber} returned to WAITING line`, 'Patient Requeued');
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
    onError: (err: any) => toast.error(err?.message || 'Failed to requeue patient'),
  });

  const entries = queueSummary?.entries || [];
  const inProgressEntry = entries.find((e) => e.status === 'IN_PROGRESS');
  const calledEntry = entries.find((e) => e.status === 'CALLED');
  const waitingEntries = entries.filter((e) => e.status === 'WAITING');
  const otherEntries = entries.filter((e) => e.status === 'SKIPPED' || e.status === 'COMPLETED');

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-text-primary tracking-tight font-heading">
              OPD Station Queue Console
            </h1>
            <Badge variant={isConnected ? 'success' : 'neutral'} size="sm">
              {isConnected ? 'Live WebSocket Connected' : 'Auto Polling'}
            </Badge>
          </div>
          <p className="text-sm text-text-secondary mt-1">
            Call patients, manage consultation sessions, and update real-time queue states.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={() => refetch()} leftIcon={<RefreshCw className="w-4 h-4" />}>
            Refresh
          </Button>
        </div>
      </div>

      {isLoading ? (
        <div className="p-16 text-center">
          <Spinner size="lg" className="text-brand-lavender mx-auto mb-2" />
          <p className="text-sm text-text-secondary">Loading real-time queue console...</p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Active Serving / Called Panel */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* IN CONSULTATION CARD */}
            <Card className="border-status-success/30 bg-status-success-surface/20">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="text-base flex items-center gap-2 text-status-success">
                    <Activity className="w-5 h-5" />
                    Current Consultation in Progress
                  </CardTitle>
                  <Badge variant="success" size="sm">
                    IN PROGRESS
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {inProgressEntry ? (
                  <div className="space-y-4">
                    <div className="flex items-baseline gap-3">
                      <span className="text-4xl font-extrabold text-status-success font-heading">
                        #{inProgressEntry.tokenNumber}
                      </span>
                      <div>
                        <p className="font-bold text-text-primary text-base">{inProgressEntry.patientName}</p>
                        <p className="text-xs text-text-secondary">
                          Ref #{inProgressEntry.appointmentReference}
                        </p>
                      </div>
                    </div>

                    <div className="text-xs text-text-secondary">
                      Started:{' '}
                      {inProgressEntry.consultationStartTimestamp
                        ? new Date(inProgressEntry.consultationStartTimestamp).toLocaleTimeString()
                        : 'Just now'}
                    </div>

                    <div className="pt-2">
                      <Button
                        variant="primary"
                        size="md"
                        className="w-full bg-status-success hover:bg-status-success/90"
                        isLoading={completeConsultMutation.isPending}
                        onClick={() => completeConsultMutation.mutate(inProgressEntry.id)}
                        leftIcon={<CheckCircle2 className="w-4 h-4" />}
                      >
                        Complete Consultation
                      </Button>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-text-muted py-6 text-center">
                    No patient consultation currently in progress. Start consultation on a called token below.
                  </p>
                )}
              </CardContent>
            </Card>

            {/* CURRENTLY CALLED CARD */}
            <Card className="border-brand-lavender/30 bg-surface-lavender/30">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="text-base flex items-center gap-2 text-brand-deep dark:text-brand-lavender">
                    <PhoneCall className="w-5 h-5 text-brand-lavender" />
                    Currently Called Token
                  </CardTitle>
                  <Badge variant="primary" size="sm">
                    CALLED
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                {calledEntry ? (
                  <div className="space-y-4">
                    <div className="flex items-baseline gap-3">
                      <span className="text-4xl font-extrabold text-brand-lavender font-heading">
                        #{calledEntry.tokenNumber}
                      </span>
                      <div>
                        <p className="font-bold text-text-primary text-base">{calledEntry.patientName}</p>
                        <p className="text-xs text-text-secondary">
                          Called at:{' '}
                          {calledEntry.calledTimestamp
                            ? new Date(calledEntry.calledTimestamp).toLocaleTimeString()
                            : 'Just now'}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 pt-2">
                      <Button
                        variant="primary"
                        size="sm"
                        className="flex-1"
                        isLoading={startConsultMutation.isPending}
                        onClick={() => startConsultMutation.mutate(calledEntry.id)}
                        leftIcon={<Play className="w-4 h-4" />}
                      >
                        Start Consultation
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        isLoading={recallEntryMutation.isPending}
                        onClick={() => recallEntryMutation.mutate(calledEntry.id)}
                      >
                        Recall
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setSkippingEntry(calledEntry)}
                      >
                        Skip
                      </Button>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-text-muted py-6 text-center">
                    No token is currently waiting in called status. Click "Call" on a waiting token below.
                  </p>
                )}
              </CardContent>
            </Card>
          </div>

          {/* WAITING QUEUE LIST */}
          <div className="space-y-4">
            <h2 className="text-lg font-bold text-text-primary tracking-tight font-heading">
              Waiting in Line ({waitingEntries.length})
            </h2>

            {waitingEntries.length === 0 ? (
              <Card className="border-surface-border">
                <CardContent className="p-8 text-center text-xs text-text-muted">
                  No patients waiting in queue at this station.
                </CardContent>
              </Card>
            ) : (
              <div className="space-y-2">
                {waitingEntries.map((entry) => (
                  <Card key={entry.id} className="border-surface-border hover:shadow-soft-sm">
                    <CardContent className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div className="flex items-center gap-3">
                        <div className="w-11 h-11 rounded-xl bg-surface-lavender text-brand-deep dark:text-brand-lavender font-extrabold flex items-center justify-center text-lg">
                          #{entry.tokenNumber}
                        </div>
                        <div>
                          <h3 className="font-bold text-text-primary text-sm">{entry.patientName}</h3>
                          <div className="flex items-center gap-2 text-xs text-text-secondary mt-0.5">
                            <span>Check-in: {new Date(entry.checkInTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                            <span>•</span>
                            <span>Priority: {entry.priorityCategory}</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 self-end sm:self-center">
                        <Button
                          variant="primary"
                          size="sm"
                          isLoading={callEntryMutation.isPending}
                          onClick={() => callEntryMutation.mutate(entry.id)}
                          leftIcon={<PhoneCall className="w-3.5 h-3.5" />}
                        >
                          Call Token
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setSkippingEntry(entry)}
                        >
                          Skip
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </div>

          {/* SKIPPED & COMPLETED SECTION */}
          {otherEntries.length > 0 && (
            <div className="space-y-4 pt-4 border-t border-surface-border">
              <h2 className="text-sm font-bold text-text-secondary uppercase tracking-wider">
                Processed / Skipped Today ({otherEntries.length})
              </h2>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {otherEntries.map((entry) => (
                  <Card key={entry.id} className="border-surface-border">
                    <CardContent className="p-3.5 flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2.5">
                        <span className="font-bold text-text-primary">#{entry.tokenNumber}</span>
                        <span className="text-text-secondary truncate">{entry.patientName}</span>
                        <Badge
                          variant={entry.status === 'COMPLETED' ? 'success' : 'neutral'}
                          size="sm"
                        >
                          {entry.status}
                        </Badge>
                      </div>

                      {entry.status === 'SKIPPED' && (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-7 text-xs"
                          isLoading={requeueMutation.isPending}
                          onClick={() => requeueMutation.mutate(entry.id)}
                          leftIcon={<RotateCcw className="w-3 h-3" />}
                        >
                          Requeue
                        </Button>
                      )}
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Skip Patient Dialog */}
      <Dialog
        isOpen={Boolean(skippingEntry)}
        onClose={() => setSkippingEntry(null)}
        title="Skip Patient Token"
      >
        <div className="space-y-4">
          <p className="text-xs text-text-secondary">
            Marking Token <strong>#{skippingEntry?.tokenNumber}</strong> ({skippingEntry?.patientName})
            as skipped. If the patient arrives later, they can be requeued back into line.
          </p>

          <div>
            <label htmlFor="skip-reason" className="block text-xs font-semibold text-text-primary mb-1">
              Reason for Absence (Optional)
            </label>
            <input
              id="skip-reason"
              type="text"
              placeholder="e.g. Called multiple times, not in waiting room..."
              value={skipReason}
              onChange={(e) => setSkipReason(e.target.value)}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-surface-border">
            <Button variant="ghost" size="sm" onClick={() => setSkippingEntry(null)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              size="sm"
              isLoading={skipEntryMutation.isPending}
              onClick={() => {
                if (skippingEntry) {
                  skipEntryMutation.mutate({
                    id: skippingEntry.id,
                    reason: skipReason || undefined,
                  });
                }
              }}
            >
              Confirm Skip
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
};
