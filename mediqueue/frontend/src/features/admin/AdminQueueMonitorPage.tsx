import React, { useState, useEffect, useCallback } from 'react';
import { adminService } from '@/lib/api/adminService';
import { AdminQueueSummaryResponse, QueueSessionResponse } from '@/types/admin';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import {
  Pause,
  Play,
  RefreshCw,
  Users,
  Activity,
  CheckCircle2,
  Hourglass,
} from 'lucide-react';

export const AdminQueueMonitorPage: React.FC = () => {
  const [queues, setQueues] = useState<AdminQueueSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Pause / Resume target
  const [selectedQueue, setSelectedQueue] = useState<AdminQueueSummaryResponse | null>(null);
  const [actionType, setActionType] = useState<'pause' | 'resume' | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const fetchQueues = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await adminService.getTodayQueues();
      setQueues(data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to query live queue sessions';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchQueues();
    // 30-second interval fallback poll for operational monitoring
    const interval = setInterval(fetchQueues, 30000);
    return () => clearInterval(interval);
  }, [fetchQueues]);

  const handleConfirmAction = async () => {
    if (!selectedQueue || !actionType) return;
    setActionLoading(true);
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
          q.sessionId === updated.id
            ? { ...q, isPaused: updated.isPaused }
            : q
        )
      );
      setSelectedQueue(null);
      setActionType(null);
      // Background refresh to synchronize all stats
      fetchQueues().catch(() => {});
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to execute queue command';
      setActionError(message);
    } finally {
      setActionLoading(false);
    }
  };

  const totalWaiting = queues.reduce((sum, q) => sum + (q.waitingCount || 0), 0);
  const totalInProgress = queues.reduce((sum, q) => sum + (q.inProgressCount || 0), 0);
  const totalCompleted = queues.reduce((sum, q) => sum + (q.completedCount || 0), 0);

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-state-success animate-pulse" />
            <span className="text-xs font-semibold text-text-muted uppercase tracking-wider">
              Real-Time OPD Session Command
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-text-primary tracking-tight mt-1">
            Hospital Queue Operations
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Monitor real-time patient queue tokens, consultation room status, and manage administrative pause overrides.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={fetchQueues}>
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Refresh Now
          </Button>
        </div>
      </div>

      {/* Aggregate Status Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <Card className="p-4 border-l-4 border-l-brand-lavender">
          <div className="flex items-center justify-between text-text-muted">
            <span className="text-xs font-semibold uppercase tracking-wider">Active Clinics</span>
            <Activity className="w-4 h-4 text-brand-lavender" />
          </div>
          <div className="mt-2 text-2xl font-bold text-text-primary">{queues.length}</div>
          <p className="text-xs text-text-muted mt-1">Supervised OPD rooms</p>
        </Card>

        <Card className="p-4 border-l-4 border-l-state-warning">
          <div className="flex items-center justify-between text-text-muted">
            <span className="text-xs font-semibold uppercase tracking-wider">Waiting in Lobby</span>
            <Hourglass className="w-4 h-4 text-state-warning" />
          </div>
          <div className="mt-2 text-2xl font-bold text-text-primary">{totalWaiting}</div>
          <p className="text-xs text-text-muted mt-1">Tokens pending call</p>
        </Card>

        <Card className="p-4 border-l-4 border-l-brand-indigo">
          <div className="flex items-center justify-between text-text-muted">
            <span className="text-xs font-semibold uppercase tracking-wider">In Consultation</span>
            <Users className="w-4 h-4 text-brand-indigo" />
          </div>
          <div className="mt-2 text-2xl font-bold text-text-primary">{totalInProgress}</div>
          <p className="text-xs text-text-muted mt-1">Patients with doctors</p>
        </Card>

        <Card className="p-4 border-l-4 border-l-state-success">
          <div className="flex items-center justify-between text-text-muted">
            <span className="text-xs font-semibold uppercase tracking-wider">Completed Today</span>
            <CheckCircle2 className="w-4 h-4 text-state-success" />
          </div>
          <div className="mt-2 text-2xl font-bold text-text-primary">{totalCompleted}</div>
          <p className="text-xs text-text-muted mt-1">Visits completed</p>
        </Card>
      </div>

      {/* Queue Sessions Grid */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <Spinner size="lg" />
          <p className="text-sm font-medium text-text-secondary">Retrieving hospital OPD sessions...</p>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load OPD Queues"
          message={error}
          onRetry={fetchQueues}
        />
      ) : queues.length === 0 ? (
        <EmptyState
          title="No queue sessions active today"
          description="OPD queue sessions are created when doctors activate their clinical schedules for the day."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {queues.map((q) => (
            <Card key={q.sessionId} className="p-5 flex flex-col justify-between border-t-4 border-t-brand-lavender">
              <div className="space-y-4">
                {/* Session Header */}
                <div className="flex items-start justify-between">
                  <div>
                    <span className="text-xs font-mono text-text-muted">Session #{q.sessionId}</span>
                    <h3 className="font-bold text-text-primary text-base mt-0.5">{q.doctorName}</h3>
                    <p className="text-xs text-brand-lavender font-medium">{q.departmentName}</p>
                  </div>
                  <div>
                    {q.isPaused ? (
                      <Badge variant="neutral">Paused</Badge>
                    ) : (
                      <Badge variant="success">Active Calling</Badge>
                    )}
                  </div>
                </div>

                {/* Token Callout Box */}
                <div className="p-3.5 rounded-xl bg-surface-subtle border border-surface-border grid grid-cols-3 text-center">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-text-muted block">In Room</span>
                    <span className="text-xl font-mono font-bold text-brand-indigo mt-0.5 block">
                      {q.currentInProgressToken ? `#${q.currentInProgressToken}` : '—'}
                    </span>
                  </div>
                  <div className="border-x border-surface-border px-2">
                    <span className="text-[10px] uppercase font-bold text-text-muted block">Last Called</span>
                    <span className="text-xl font-mono font-bold text-brand-lavender mt-0.5 block">
                      {q.currentCalledToken ? `#${q.currentCalledToken}` : '—'}
                    </span>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-text-muted block">Total Issued</span>
                    <span className="text-xl font-mono font-bold text-text-primary mt-0.5 block">
                      #{q.lastTokenNumber}
                    </span>
                  </div>
                </div>

                {/* Breakdown Stats */}
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="p-2 rounded-lg bg-surface-elevated border border-surface-border flex items-center justify-between">
                    <span className="text-text-muted">Waiting:</span>
                    <span className="font-bold text-state-warning">{q.waitingCount}</span>
                  </div>
                  <div className="p-2 rounded-lg bg-surface-elevated border border-surface-border flex items-center justify-between">
                    <span className="text-text-muted">Completed:</span>
                    <span className="font-bold text-state-success">{q.completedCount}</span>
                  </div>
                  <div className="p-2 rounded-lg bg-surface-elevated border border-surface-border flex items-center justify-between">
                    <span className="text-text-muted">Skipped:</span>
                    <span className="font-bold text-text-muted">{q.skippedCount}</span>
                  </div>
                  <div className="p-2 rounded-lg bg-surface-elevated border border-surface-border flex items-center justify-between">
                    <span className="text-text-muted">Cancelled:</span>
                    <span className="font-bold text-state-error">{q.cancelledCount}</span>
                  </div>
                </div>
              </div>

              {/* Action Footer */}
              <div className="mt-5 pt-3 border-t border-surface-border flex items-center justify-between">
                <span className="text-xs text-text-muted">
                  {q.totalPatients} patients enrolled
                </span>
                {q.isPaused ? (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => {
                      setSelectedQueue(q);
                      setActionType('resume');
                    }}
                  >
                    <Play className="w-3.5 h-3.5 mr-1" />
                    Resume Session
                  </Button>
                ) : (
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => {
                      setSelectedQueue(q);
                      setActionType('pause');
                    }}
                  >
                    <Pause className="w-3.5 h-3.5 mr-1 text-state-warning" />
                    Pause Session
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Confirmation Dialog */}
      <ConfirmDialog
        isOpen={Boolean(selectedQueue && actionType)}
        title={actionType === 'pause' ? 'Confirm Session Pause' : 'Confirm Session Resume'}
        message={
          actionType === 'pause'
            ? `Pause queue session #${selectedQueue?.sessionId} (${selectedQueue?.doctorName})? Patients will be notified that token calling is temporarily held.`
            : `Resume queue session #${selectedQueue?.sessionId} (${selectedQueue?.doctorName})? Normal token progression will resume immediately.`
        }
        confirmText={actionType === 'pause' ? 'Pause Session' : 'Resume Session'}
        variant={actionType === 'pause' ? 'danger' : 'primary'}
        isLoading={actionLoading}
        onConfirm={handleConfirmAction}
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
