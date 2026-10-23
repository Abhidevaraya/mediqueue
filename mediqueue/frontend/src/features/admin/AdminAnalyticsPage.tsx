import React, { useState, useEffect, useCallback } from 'react';
import { adminService } from '@/lib/api/adminService';
import {
  AppointmentAnalyticsResponse,
  WaitTimeAnalyticsResponse,
  DoctorWorkloadResponse,
  DepartmentWorkloadResponse,
} from '@/types/admin';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import {
  Download,
  Calendar,
  RefreshCw,
} from 'lucide-react';

export const AdminAnalyticsPage: React.FC = () => {
  // Date range defaults to last 30 days
  const todayStr = new Date().toISOString().split('T')[0];
  const thirtyDaysAgoStr = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)
    .toISOString()
    .split('T')[0];

  const [fromDate, setFromDate] = useState(thirtyDaysAgoStr);
  const [toDate, setToDate] = useState(todayStr);

  const [appointments, setAppointments] = useState<AppointmentAnalyticsResponse | null>(null);
  const [waitTimes, setWaitTimes] = useState<WaitTimeAnalyticsResponse | null>(null);
  const [doctorWorkloads, setDoctorWorkloads] = useState<DoctorWorkloadResponse[]>([]);
  const [departmentWorkloads, setDepartmentWorkloads] = useState<DepartmentWorkloadResponse[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [exportingAppts, setExportingAppts] = useState(false);
  const [exportingWaitTimes, setExportingWaitTimes] = useState(false);

  const fetchAnalytics = useCallback(async () => {
    if (fromDate > toDate) {
      setError('Start date cannot be after end date.');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [appts, waits, docLoads, deptLoads] = await Promise.all([
        adminService.getAppointmentAnalytics({ from: fromDate, to: toDate }),
        adminService.getWaitTimeAnalytics({ from: fromDate, to: toDate }),
        adminService.getDoctorWorkload({ from: fromDate, to: toDate }),
        adminService.getDepartmentWorkload({ from: fromDate, to: toDate }),
      ]);
      setAppointments(appts);
      setWaitTimes(waits);
      setDoctorWorkloads(docLoads);
      setDepartmentWorkloads(deptLoads);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to retrieve hospital analytics';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [fromDate, toDate]);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  const handleExportAppointments = async () => {
    setExportingAppts(true);
    try {
      const blob = await adminService.exportAppointmentsCsv({ from: fromDate, to: toDate });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `appointments_analytics_${fromDate}_to_${toDate}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to export appointments CSV');
    } finally {
      setExportingAppts(false);
    }
  };

  const handleExportWaitTimes = async () => {
    setExportingWaitTimes(true);
    try {
      const blob = await adminService.exportWaitTimesCsv({ from: fromDate, to: toDate });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `waittimes_analytics_${fromDate}_to_${toDate}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to export wait times CSV');
    } finally {
      setExportingWaitTimes(false);
    }
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-brand-lavender/10 text-brand-lavender">
              BI & Hospital Intelligence
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-text-primary tracking-tight mt-1">
            Clinical Operations & Queue Analytics
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Authoritative performance reporting, wait-time prediction accuracy, OPD workload, and data exports.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleExportAppointments}
            isLoading={exportingAppts}
          >
            <Download className="w-4 h-4 mr-1.5" />
            Export Appointments CSV
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={handleExportWaitTimes}
            isLoading={exportingWaitTimes}
          >
            <Download className="w-4 h-4 mr-1.5" />
            Export Wait Times CSV
          </Button>
        </div>
      </div>

      {/* Date Range Selector */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4 text-brand-lavender" />
              <label htmlFor="from-date" className="text-xs font-semibold text-text-muted uppercase">From</label>
              <input
                id="from-date"
                type="date"
                value={fromDate}
                max={toDate}
                onChange={(e) => setFromDate(e.target.value)}
                className="px-3 py-1.5 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
            <div className="flex items-center gap-2">
              <label htmlFor="to-date" className="text-xs font-semibold text-text-muted uppercase">To</label>
              <input
                id="to-date"
                type="date"
                value={toDate}
                min={fromDate}
                onChange={(e) => setToDate(e.target.value)}
                className="px-3 py-1.5 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-text-muted">
              Period: <strong className="text-text-primary">{fromDate}</strong> to <strong className="text-text-primary">{toDate}</strong>
            </span>
            <Button variant="ghost" size="sm" onClick={fetchAnalytics}>
              <RefreshCw className="w-3.5 h-3.5 mr-1" />
              Apply
            </Button>
          </div>
        </div>
      </Card>

      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 gap-3">
          <Spinner size="lg" />
          <p className="text-sm font-medium text-text-secondary">Computing operational analytics...</p>
        </div>
      ) : error ? (
        <ErrorState
          title="Analytics Error"
          message={error}
          onRetry={fetchAnalytics}
        />
      ) : (
        <>
          {/* Key Performance Indicators */}
          {appointments && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <Card className="p-5 border-l-4 border-l-brand-lavender">
                <span className="text-xs font-semibold text-text-muted uppercase">Total Volume</span>
                <div className="mt-2 text-2xl font-bold text-text-primary">
                  {appointments.totalAppointments}
                </div>
                <div className="mt-2 flex items-center gap-2 text-xs text-text-muted">
                  <span className="text-brand-lavender font-semibold">{appointments.bookedCount} booked</span>
                  <span>&bull;</span>
                  <span>{appointments.checkedInCount} checked-in</span>
                </div>
              </Card>

              <Card className="p-5 border-l-4 border-l-state-success">
                <span className="text-xs font-semibold text-text-muted uppercase">Attendance Rate</span>
                <div className="mt-2 flex items-baseline gap-2">
                  <span className="text-2xl font-bold text-state-success">
                    {appointments.attendanceRatePercent}%
                  </span>
                  <span className="text-xs text-text-muted">({appointments.completedCount} completed)</span>
                </div>
                <p className="text-xs text-text-muted mt-2">Visits successfully attended</p>
              </Card>

              <Card className="p-5 border-l-4 border-l-state-error">
                <span className="text-xs font-semibold text-text-muted uppercase">Cancellation Rate</span>
                <div className="mt-2 flex items-baseline gap-2">
                  <span className="text-2xl font-bold text-state-error">
                    {appointments.cancellationRatePercent}%
                  </span>
                  <span className="text-xs text-text-muted">({appointments.cancelledCount} cancelled)</span>
                </div>
                <p className="text-xs text-text-muted mt-2">Patient or clinic cancelled</p>
              </Card>

              <Card className="p-5 border-l-4 border-l-brand-indigo">
                <span className="text-xs font-semibold text-text-muted uppercase">No-Show Records</span>
                <div className="mt-2 text-2xl font-bold text-brand-indigo">
                  {appointments.noShowCount}
                </div>
                <p className="text-xs text-text-muted mt-2">Missed scheduled consultations</p>
              </Card>
            </div>
          )}

          {/* Wait Time Analytics & Distribution */}
          {waitTimes && (
            <Card className="p-6">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
                <div>
                  <h2 className="text-lg font-bold text-text-primary">Patient Wait-Time Analytics</h2>
                  <p className="text-xs text-text-secondary mt-0.5">
                    Empirical arrival-to-consultation intervals across {waitTimes.totalSamples} recorded visits.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant="outline">
                    Prediction Error: &plusmn;{waitTimes.meanAbsolutePredictionErrorMinutes} mins
                  </Badge>
                </div>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
                <div className="p-4 rounded-xl bg-surface-subtle border border-surface-border text-center">
                  <span className="text-xs text-text-muted font-semibold uppercase block">Average Wait</span>
                  <span className="text-2xl font-bold text-brand-lavender mt-1 block">
                    {waitTimes.averageWaitTimeMinutes}m
                  </span>
                  <span className="text-[11px] text-text-muted">Mean interval</span>
                </div>

                <div className="p-4 rounded-xl bg-surface-subtle border border-surface-border text-center">
                  <span className="text-xs text-text-muted font-semibold uppercase block">Median (P50)</span>
                  <span className="text-2xl font-bold text-text-primary mt-1 block">
                    {waitTimes.medianWaitTimeMinutes}m
                  </span>
                  <span className="text-[11px] text-text-muted">50% wait under this</span>
                </div>

                <div className="p-4 rounded-xl bg-surface-subtle border border-surface-border text-center">
                  <span className="text-xs text-text-muted font-semibold uppercase block">90th Percentile</span>
                  <span className="text-2xl font-bold text-state-warning mt-1 block">
                    {waitTimes.p90WaitTimeMinutes}m
                  </span>
                  <span className="text-[11px] text-text-muted">Worst-case queue buffer</span>
                </div>

                <div className="p-4 rounded-xl bg-surface-subtle border border-surface-border text-center">
                  <span className="text-xs text-text-muted font-semibold uppercase block">Minimum</span>
                  <span className="text-2xl font-bold text-state-success mt-1 block">
                    {waitTimes.minWaitTimeMinutes}m
                  </span>
                  <span className="text-[11px] text-text-muted">Fastest turnaround</span>
                </div>

                <div className="p-4 rounded-xl bg-surface-subtle border border-surface-border text-center">
                  <span className="text-xs text-text-muted font-semibold uppercase block">Maximum</span>
                  <span className="text-2xl font-bold text-state-error mt-1 block">
                    {waitTimes.maxWaitTimeMinutes}m
                  </span>
                  <span className="text-[11px] text-text-muted">Peak surge delay</span>
                </div>
              </div>

              {/* Prediction Breakdown Badges */}
              {waitTimes.predictionMethodBreakdown && Object.keys(waitTimes.predictionMethodBreakdown).length > 0 && (
                <div className="mt-6 pt-4 border-t border-surface-border">
                  <span className="text-xs font-semibold text-text-muted uppercase block mb-2">
                    Estimation Algorithm Usage
                  </span>
                  <div className="flex flex-wrap gap-2">
                    {Object.entries(waitTimes.predictionMethodBreakdown).map(([algo, count]) => (
                      <span
                        key={algo}
                        className="px-2.5 py-1 rounded-lg bg-surface-elevated border border-surface-border text-xs text-text-secondary"
                      >
                        <strong className="text-text-primary">{algo}</strong>: {count} estimates
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </Card>
          )}

          {/* Daily Appointment Trend Breakdown */}
          {appointments && appointments.dailyBreakdown.length > 0 && (
            <Card className="p-6">
              <h2 className="text-lg font-bold text-text-primary mb-1">Daily Appointment Breakdown</h2>
              <p className="text-xs text-text-secondary mb-4">
                Daily volume trends over the selected reporting window.
              </p>

              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-sm">
                  <thead>
                    <tr className="border-b border-surface-border bg-surface-subtle/50 text-xs font-semibold text-text-muted uppercase tracking-wider">
                      <th className="px-4 py-2.5">Date</th>
                      <th className="px-4 py-2.5">Total Bookings</th>
                      <th className="px-4 py-2.5">Completed</th>
                      <th className="px-4 py-2.5">Cancelled</th>
                      <th className="px-4 py-2.5">Booked / Pending</th>
                      <th className="px-4 py-2.5 text-right">Completion Rate</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-surface-border">
                    {appointments.dailyBreakdown.map((row) => {
                      const rate = row.total > 0 ? Math.round((row.completed / row.total) * 100) : 0;
                      return (
                        <tr key={row.date} className="hover:bg-surface-subtle/40 transition-colors">
                          <td className="px-4 py-3 font-mono text-xs text-text-primary">{row.date}</td>
                          <td className="px-4 py-3 font-bold text-text-primary">{row.total}</td>
                          <td className="px-4 py-3 text-state-success font-medium">{row.completed}</td>
                          <td className="px-4 py-3 text-state-error font-medium">{row.cancelled}</td>
                          <td className="px-4 py-3 text-brand-lavender font-medium">{row.booked}</td>
                          <td className="px-4 py-3 text-right">
                            <span className="font-mono text-xs font-semibold">{rate}%</span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </Card>
          )}

          {/* Department Workload Distribution */}
          <Card className="p-6">
            <h2 className="text-lg font-bold text-text-primary mb-1">Department OPD Workload</h2>
            <p className="text-xs text-text-secondary mb-4">
              Patient booking throughput and active doctor staffing per hospital unit.
            </p>

            {departmentWorkloads.length === 0 ? (
              <p className="text-xs text-text-muted italic">No department workload records for this period.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-sm">
                  <thead>
                    <tr className="border-b border-surface-border bg-surface-subtle/50 text-xs font-semibold text-text-muted uppercase tracking-wider">
                      <th className="px-4 py-2.5">Department</th>
                      <th className="px-4 py-2.5">Active Staff</th>
                      <th className="px-4 py-2.5">Total Appointments</th>
                      <th className="px-4 py-2.5">Completed</th>
                      <th className="px-4 py-2.5">Cancelled</th>
                      <th className="px-4 py-2.5 text-right">Throughput</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-surface-border">
                    {departmentWorkloads.map((dept) => {
                      const throughput =
                        dept.totalAppointments > 0
                          ? Math.round((dept.completedAppointments / dept.totalAppointments) * 100)
                          : 0;
                      return (
                        <tr key={dept.departmentId} className="hover:bg-surface-subtle/40 transition-colors">
                          <td className="px-4 py-3 font-semibold text-text-primary">{dept.departmentName}</td>
                          <td className="px-4 py-3 text-text-secondary">{dept.activeDoctorsCount} doctors</td>
                          <td className="px-4 py-3 font-bold text-text-primary">{dept.totalAppointments}</td>
                          <td className="px-4 py-3 text-state-success font-medium">{dept.completedAppointments}</td>
                          <td className="px-4 py-3 text-state-error font-medium">{dept.cancelledAppointments}</td>
                          <td className="px-4 py-3 text-right font-mono text-xs font-semibold">
                            {throughput}%
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          {/* Doctor Productivity & Consultation Duration */}
          <Card className="p-6">
            <h2 className="text-lg font-bold text-text-primary mb-1">Doctor Clinical Workload & Duration</h2>
            <p className="text-xs text-text-secondary mb-4">
              Practitioner consultation volumes and average encounter durations.
            </p>

            {doctorWorkloads.length === 0 ? (
              <p className="text-xs text-text-muted italic">No doctor workload records for this period.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse text-sm">
                  <thead>
                    <tr className="border-b border-surface-border bg-surface-subtle/50 text-xs font-semibold text-text-muted uppercase tracking-wider">
                      <th className="px-4 py-2.5">Doctor</th>
                      <th className="px-4 py-2.5">Department & Specialty</th>
                      <th className="px-4 py-2.5">Total Bookings</th>
                      <th className="px-4 py-2.5">Completed Consultations</th>
                      <th className="px-4 py-2.5">Avg Duration</th>
                      <th className="px-4 py-2.5 text-right">Median Duration</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-surface-border">
                    {doctorWorkloads.map((doc) => (
                      <tr key={doc.doctorId} className="hover:bg-surface-subtle/40 transition-colors">
                        <td className="px-4 py-3 font-medium text-text-primary">{doc.doctorName}</td>
                        <td className="px-4 py-3 text-xs text-text-secondary">
                          <span>{doc.departmentName}</span> &bull;{' '}
                          <span className="text-brand-lavender">{doc.specialty}</span>
                        </td>
                        <td className="px-4 py-3 font-bold text-text-primary">{doc.totalAppointments}</td>
                        <td className="px-4 py-3 text-state-success font-medium">{doc.completedConsultations}</td>
                        <td className="px-4 py-3 font-mono text-xs">{doc.averageConsultationMinutes} mins</td>
                        <td className="px-4 py-3 text-right font-mono text-xs font-medium">
                          {doc.medianConsultationMinutes} mins
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </>
      )}
    </div>
  );
};
