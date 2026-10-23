import React, { useState, useEffect, useCallback } from 'react';
import { adminService } from '@/lib/api/adminService';
import { ClinicalAuditEvent } from '@/types/admin';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import {
  Download,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Lock,
} from 'lucide-react';

export const AdminAuditLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<ClinicalAuditEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [actorRole, setActorRole] = useState<string>('');
  const [actionQuery, setActionQuery] = useState<string>('');
  const [resourceType, setResourceType] = useState<string>('');
  const [outcome, setOutcome] = useState<string>('');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');

  // Pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [exporting, setExporting] = useState(false);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await adminService.getAuditLogs({
        actorRole: actorRole || undefined,
        action: actionQuery.trim() || undefined,
        resourceType: resourceType.trim() || undefined,
        outcome: outcome || undefined,
        from: fromDate || undefined,
        to: toDate || undefined,
        page,
        size: 20,
      });
      setLogs(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to retrieve clinical audit logs';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [actorRole, actionQuery, resourceType, outcome, fromDate, toDate, page]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleExportCsv = async () => {
    setExporting(true);
    try {
      const blob = await adminService.exportAuditLogsCsv({
        actorRole: actorRole || undefined,
        action: actionQuery.trim() || undefined,
        resourceType: resourceType.trim() || undefined,
        outcome: outcome || undefined,
        from: fromDate || undefined,
        to: toDate || undefined,
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `clinical_audit_log_${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to export audit logs CSV');
    } finally {
      setExporting(false);
    }
  };

  const getOutcomeBadge = (eventOutcome: string) => {
    const isSuccess = eventOutcome.toUpperCase().includes('SUCCESS');
    return (
      <Badge variant={isSuccess ? 'success' : 'error'}>
        {eventOutcome}
      </Badge>
    );
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-brand-indigo/10 text-brand-indigo flex items-center gap-1">
              <Lock className="w-3 h-3" />
              HIPAA & Security Compliance
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-text-primary tracking-tight mt-1">
            Clinical System Audit Trail
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Immutable log of clinical appointments, queue overrides, access verification, and administrative changes.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={fetchLogs}>
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Refresh
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={handleExportCsv}
            isLoading={exporting}
          >
            <Download className="w-4 h-4 mr-1.5" />
            Export Audit CSV
          </Button>
        </div>
      </div>

      {/* Filter Bar */}
      <Card className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3">
          <div>
            <label htmlFor="actor-role" className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Actor Role
            </label>
            <select
              id="actor-role"
              value={actorRole}
              onChange={(e) => {
                setActorRole(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            >
              <option value="">All Roles</option>
              <option value="ADMIN">ADMIN</option>
              <option value="DOCTOR">DOCTOR</option>
              <option value="PATIENT">PATIENT</option>
            </select>
          </div>

          <div>
            <label htmlFor="action-filter" className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Action Name
            </label>
            <input
              id="action-filter"
              type="text"
              placeholder="e.g. APPOINTMENT_CREATED"
              value={actionQuery}
              onChange={(e) => {
                setActionQuery(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>

          <div>
            <label htmlFor="resource-type" className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Resource Type
            </label>
            <input
              id="resource-type"
              type="text"
              placeholder="e.g. APPOINTMENT, QUEUE"
              value={resourceType}
              onChange={(e) => {
                setResourceType(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>

          <div>
            <label htmlFor="outcome-filter" className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Outcome
            </label>
            <select
              id="outcome-filter"
              value={outcome}
              onChange={(e) => {
                setOutcome(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            >
              <option value="">All Outcomes</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="FAILURE">FAILURE</option>
            </select>
          </div>

          <div>
            <label htmlFor="audit-start-date" className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Start Date
            </label>
            <input
              id="audit-start-date"
              type="date"
              value={fromDate}
              onChange={(e) => {
                setFromDate(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>

          <div>
            <label htmlFor="audit-end-date" className="block text-xs font-semibold text-text-muted uppercase mb-1">
              End Date
            </label>
            <input
              id="audit-end-date"
              type="date"
              value={toDate}
              onChange={(e) => {
                setToDate(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>
        </div>
      </Card>

      {/* Audit Log Table */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <Spinner size="lg" />
          <p className="text-sm font-medium text-text-secondary">Querying audit events...</p>
        </div>
      ) : error ? (
        <ErrorState
          title="Audit Log Query Failed"
          message={error}
          onRetry={fetchLogs}
        />
      ) : logs.length === 0 ? (
        <EmptyState
          title="No audit events matched"
          description="Try broadening your filter criteria or date range to inspect audit events."
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-surface-border bg-surface-subtle/50 text-xs font-semibold text-text-muted uppercase tracking-wider">
                  <th className="px-5 py-3">Event ID</th>
                  <th className="px-5 py-3">Timestamp</th>
                  <th className="px-5 py-3">Actor</th>
                  <th className="px-5 py-3">Action</th>
                  <th className="px-5 py-3">Resource</th>
                  <th className="px-5 py-3">Outcome</th>
                  <th className="px-5 py-3">Context / Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-border">
                {logs.map((evt) => (
                  <tr key={evt.id} className="hover:bg-surface-subtle/40 transition-colors">
                    <td className="px-5 py-3.5 font-mono text-xs text-text-muted">
                      #{evt.id}
                    </td>
                    <td className="px-5 py-3.5 font-mono text-xs text-text-secondary whitespace-nowrap">
                      {new Date(evt.eventTimestamp).toLocaleString()}
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="font-semibold text-xs text-text-primary">
                        {evt.actorRole || 'SYSTEM'}
                      </div>
                      {evt.actorUserId && (
                        <div className="text-[11px] text-text-muted font-mono">
                          User #{evt.actorUserId}
                        </div>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="font-mono text-xs font-semibold text-brand-lavender">
                        {evt.action}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="text-xs font-medium text-text-primary">
                        {evt.resourceType}
                      </div>
                      {evt.resourceId && (
                        <div className="text-[11px] text-text-muted font-mono">
                          ID: #{evt.resourceId}
                        </div>
                      )}
                      {evt.patientId && (
                        <div className="text-[11px] text-brand-indigo font-mono">
                          Patient: #{evt.patientId}
                        </div>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      {getOutcomeBadge(evt.outcome)}
                    </td>
                    <td className="px-5 py-3.5 text-xs text-text-secondary max-w-xs truncate">
                      {evt.safeReason || '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="p-4 border-t border-surface-border flex items-center justify-between">
              <span className="text-xs text-text-muted">
                Showing page {page + 1} of {totalPages} ({totalElements} recorded events)
              </span>
              <div className="flex items-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  <ChevronLeft className="w-4 h-4" />
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  <ChevronRight className="w-4 h-4" />
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}
    </div>
  );
};
