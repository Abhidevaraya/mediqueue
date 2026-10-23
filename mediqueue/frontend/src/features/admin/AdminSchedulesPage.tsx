import React, { useState, useEffect, useCallback } from 'react';
import { adminService } from '@/lib/api/adminService';
import { DoctorScheduleResponse, DepartmentResponse } from '@/types/doctor';
import { DoctorAdminResponse } from '@/types/admin';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import {
  Coffee,
  RefreshCw,
} from 'lucide-react';

const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

export const AdminSchedulesPage: React.FC = () => {
  const [schedules, setSchedules] = useState<DoctorScheduleResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [doctors, setDoctors] = useState<DoctorAdminResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [selectedDeptId, setSelectedDeptId] = useState<number | ''>('');
  const [selectedDoctorId, setSelectedDoctorId] = useState<number | ''>('');
  const [selectedDay, setSelectedDay] = useState<string>('');

  const fetchMetaAndSchedules = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [deptList, docList, scheduleList] = await Promise.all([
        adminService.getDepartments(),
        adminService.getDoctors({ page: 0, size: 100 }),
        adminService.getSchedules({
          departmentId: selectedDeptId !== '' ? Number(selectedDeptId) : undefined,
          doctorId: selectedDoctorId !== '' ? Number(selectedDoctorId) : undefined,
          dayOfWeek: selectedDay || undefined,
        }),
      ]);
      setDepartments(Array.isArray(deptList) ? deptList : []);
      const doctorsArray = docList && 'content' in docList && Array.isArray(docList.content)
        ? docList.content
        : Array.isArray(docList)
        ? docList
        : [];
      setDoctors(doctorsArray);
      setSchedules(Array.isArray(scheduleList) ? scheduleList : []);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to query doctor rosters';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [selectedDeptId, selectedDoctorId, selectedDay]);

  useEffect(() => {
    fetchMetaAndSchedules();
  }, [fetchMetaAndSchedules]);

  // Map doctor names for easy display
  const doctorMap = new Map(doctors.map((d) => [d.id, d]));

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-text-primary tracking-tight">
            Doctor Schedules & Rosters
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Hospital-wide operational review of clinical shifts, consultation slot intervals, and planned break windows.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={fetchMetaAndSchedules}>
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Refresh
          </Button>
        </div>
      </div>

      {/* Filter Bar */}
      <Card className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Department
            </label>
            <select
              value={selectedDeptId}
              onChange={(e) => setSelectedDeptId(e.target.value ? Number(e.target.value) : '')}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            >
              <option value="">All Departments</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Specialist Doctor
            </label>
            <select
              value={selectedDoctorId}
              onChange={(e) => setSelectedDoctorId(e.target.value ? Number(e.target.value) : '')}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            >
              <option value="">All Doctors</option>
              {doctors.map((doc) => (
                <option key={doc.id} value={doc.id}>
                  {doc.fullName} ({doc.specialty})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Day of Week
            </label>
            <select
              value={selectedDay}
              onChange={(e) => setSelectedDay(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            >
              <option value="">All Days</option>
              {DAYS_OF_WEEK.map((day) => (
                <option key={day} value={day}>
                  {day}
                </option>
              ))}
            </select>
          </div>
        </div>
      </Card>

      {/* Main Roster List */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <Spinner size="lg" />
          <p className="text-sm font-medium text-text-secondary">Loading clinical rosters...</p>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Schedules"
          message={error}
          onRetry={fetchMetaAndSchedules}
        />
      ) : schedules.length === 0 ? (
        <EmptyState
          title="No doctor schedules matched"
          description="Try broadening your department or day filter to inspect rosters."
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-surface-border bg-surface-subtle/50 text-xs font-semibold text-text-muted uppercase tracking-wider">
                  <th className="px-5 py-3">Schedule ID</th>
                  <th className="px-5 py-3">Doctor</th>
                  <th className="px-5 py-3">Day of Week</th>
                  <th className="px-5 py-3">Clinic Hours</th>
                  <th className="px-5 py-3">Slot Duration</th>
                  <th className="px-5 py-3">Break Window</th>
                  <th className="px-5 py-3">Roster Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-border">
                {schedules.map((sch) => {
                  const doc = doctorMap.get(sch.doctorId);
                  return (
                    <tr key={sch.id} className="hover:bg-surface-subtle/40 transition-colors">
                      <td className="px-5 py-3.5 font-mono text-xs text-text-muted">
                        #{sch.id}
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="font-medium text-text-primary">
                          {doc ? doc.fullName : `Doctor #${sch.doctorId}`}
                        </div>
                        {doc && (
                          <div className="text-xs text-brand-lavender font-medium">
                            {doc.departmentName} &bull; {doc.specialty}
                          </div>
                        )}
                      </td>
                      <td className="px-5 py-3.5">
                        <Badge variant="primary">{sch.dayOfWeek}</Badge>
                      </td>
                      <td className="px-5 py-3.5 font-mono text-sm text-text-primary">
                        {sch.startTime} &ndash; {sch.endTime}
                      </td>
                      <td className="px-5 py-3.5 text-text-secondary">
                        {sch.slotDurationMinutes} mins / slot
                      </td>
                      <td className="px-5 py-3.5 text-xs text-text-secondary">
                        {sch.breakStartTime && sch.breakEndTime ? (
                          <div className="flex items-center gap-1 font-mono">
                            <Coffee className="w-3.5 h-3.5 text-text-muted" />
                            <span>
                              {sch.breakStartTime} &ndash; {sch.breakEndTime}
                            </span>
                          </div>
                        ) : (
                          <span className="text-text-muted italic">No scheduled break</span>
                        )}
                      </td>
                      <td className="px-5 py-3.5">
                        <Badge variant={sch.isActive ? 'success' : 'neutral'}>
                          {sch.isActive ? 'Active Roster' : 'Inactive'}
                        </Badge>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </div>
  );
};
