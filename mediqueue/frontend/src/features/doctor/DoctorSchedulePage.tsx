import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/lib/feedback/ToastContext';
import { doctorService } from '@/lib/api/doctorService';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { Dialog } from '@/components/ui/Dialog';
import {
  Clock,
  Plus,
  Coffee,
  Palmtree,
} from 'lucide-react';

export const DoctorSchedulePage: React.FC = () => {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  const [isAddScheduleOpen, setIsAddScheduleOpen] = useState(false);
  const [isAddExceptionOpen, setIsAddExceptionOpen] = useState(false);

  // Form states for new schedule
  const [dayOfWeek, setDayOfWeek] = useState('MONDAY');
  const [startTime, setStartTime] = useState('09:00:00');
  const [endTime, setEndTime] = useState('17:00:00');
  const [slotDurationMinutes, setSlotDurationMinutes] = useState(20);
  const [breakStartTime, setBreakStartTime] = useState('13:00:00');
  const [breakEndTime, setBreakEndTime] = useState('14:00:00');

  // Form states for exception
  const [exceptionDate, setExceptionDate] = useState(
    new Date(Date.now() + 86400000).toISOString().split('T')[0]
  );
  const [exceptionReason, setExceptionReason] = useState('Annual Leave');

  // Schedules query
  const {
    data: schedules,
    isLoading: isSchedulesLoading,
  } = useQuery({
    queryKey: ['doctor', 'schedules', user?.id],
    queryFn: () => doctorService.getDoctorSchedules(user!.id),
    enabled: Boolean(user?.id),
  });

  // Exceptions query
  const {
    data: exceptions,
    isLoading: isExceptionsLoading,
  } = useQuery({
    queryKey: ['doctor', 'exceptions'],
    queryFn: () => doctorService.getExceptions(),
  });

  // Add schedule mutation
  const addScheduleMutation = useMutation({
    mutationFn: doctorService.createSchedule,
    onSuccess: () => {
      toast.success('Weekly working schedule interval saved', 'Schedule Saved');
      queryClient.invalidateQueries({ queryKey: ['doctor', 'schedules'] });
      setIsAddScheduleOpen(false);
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to save schedule interval');
    },
  });

  // Add exception mutation
  const addExceptionMutation = useMutation({
    mutationFn: doctorService.createException,
    onSuccess: () => {
      toast.success('Schedule exception recorded', 'Exception Added');
      queryClient.invalidateQueries({ queryKey: ['doctor', 'exceptions'] });
      setIsAddExceptionOpen(false);
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to record schedule exception');
    },
  });

  const daysOrder = [
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY',
  ];

  const sortedSchedules = [...(schedules || [])].sort(
    (a, b) => daysOrder.indexOf(a.dayOfWeek) - daysOrder.indexOf(b.dayOfWeek)
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <h1 className="text-2xl font-bold text-text-primary tracking-tight font-heading">
            Doctor Schedule & Availability
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Configure weekly recurring working shifts, break periods, and holiday leaves.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsAddExceptionOpen(true)}
            leftIcon={<Palmtree className="w-4 h-4" />}
          >
            Record Leave / Exception
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => setIsAddScheduleOpen(true)}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Add Working Hours
          </Button>
        </div>
      </div>

      {/* Weekly Working Shifts */}
      <div className="space-y-4">
        <h2 className="text-lg font-bold text-text-primary tracking-tight font-heading">
          Published Weekly Working Hours
        </h2>

        {isSchedulesLoading ? (
          <div className="p-12 text-center">
            <Spinner size="md" className="text-brand-lavender mx-auto mb-2" />
            <p className="text-xs text-text-secondary">Loading weekly schedules...</p>
          </div>
        ) : sortedSchedules.length === 0 ? (
          <Card className="border-surface-border">
            <CardContent className="p-8 text-center text-xs text-text-muted">
              No weekly working hours published. Click "Add Working Hours" to configure shifts.
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {sortedSchedules.map((sch) => (
              <Card key={sch.id} className="border-surface-border hover:shadow-soft-sm transition-shadow">
                <CardContent className="p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="font-extrabold text-sm text-brand-deep dark:text-brand-lavender font-heading">
                      {sch.dayOfWeek}
                    </span>
                    <Badge variant={sch.isActive ? 'success' : 'neutral'} size="sm">
                      {sch.isActive ? 'ACTIVE' : 'INACTIVE'}
                    </Badge>
                  </div>

                  <div className="space-y-1.5 text-xs">
                    <div className="flex items-center gap-2 text-text-primary font-medium">
                      <Clock className="w-4 h-4 text-brand-lavender" />
                      <span>
                        Shift: {sch.startTime.substring(0, 5)} – {sch.endTime.substring(0, 5)}
                      </span>
                    </div>

                    {sch.breakStartTime && sch.breakEndTime && (
                      <div className="flex items-center gap-2 text-text-secondary">
                        <Coffee className="w-4 h-4 text-text-muted" />
                        <span>
                          Break: {sch.breakStartTime.substring(0, 5)} – {sch.breakEndTime.substring(0, 5)}
                        </span>
                      </div>
                    )}

                    <div className="text-text-muted pt-1">
                      Slot Duration: {sch.slotDurationMinutes} mins / appointment
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Schedule Exceptions & Leaves */}
      <div className="space-y-4 pt-4 border-t border-surface-border">
        <h2 className="text-lg font-bold text-text-primary tracking-tight font-heading">
          Schedule Exceptions & Leave Dates ({exceptions?.length ?? 0})
        </h2>

        {isExceptionsLoading ? (
          <div className="p-8 text-center">
            <Spinner size="sm" className="text-brand-lavender mx-auto mb-2" />
          </div>
        ) : !exceptions || exceptions.length === 0 ? (
          <Card className="border-surface-border">
            <CardContent className="p-6 text-center text-xs text-text-muted">
              No leave exceptions recorded.
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {exceptions.map((exc) => (
              <Card key={exc.id} className="border-surface-border">
                <CardContent className="p-4 flex items-center justify-between gap-3 text-xs">
                  <div>
                    <span className="font-bold text-text-primary block">{exc.exceptionDate}</span>
                    <span className="text-text-secondary mt-0.5 block">{exc.reason || 'Exception'}</span>
                  </div>
                  <Badge variant={exc.isAvailable ? 'primary' : 'error'} size="sm">
                    {exc.isAvailable ? 'CUSTOM HOURS' : 'UNAVAILABLE'}
                  </Badge>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Add Schedule Dialog */}
      <Dialog
        isOpen={isAddScheduleOpen}
        onClose={() => setIsAddScheduleOpen(false)}
        title="Add Working Schedule Shift"
      >
        <div className="space-y-4">
          <div>
            <label htmlFor="day-of-week" className="block text-xs font-semibold text-text-primary mb-1">Day of Week</label>
            <select
              id="day-of-week"
              value={dayOfWeek}
              onChange={(e) => setDayOfWeek(e.target.value)}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            >
              {daysOrder.map((day) => (
                <option key={day} value={day}>
                  {day}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="shift-start" className="block text-xs font-semibold text-text-primary mb-1">Start Time</label>
              <input
                id="shift-start"
                type="time"
                value={startTime.substring(0, 5)}
                onChange={(e) => setStartTime(`${e.target.value}:00`)}
                className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
              />
            </div>
            <div>
              <label htmlFor="shift-end" className="block text-xs font-semibold text-text-primary mb-1">End Time</label>
              <input
                id="shift-end"
                type="time"
                value={endTime.substring(0, 5)}
                onChange={(e) => setEndTime(`${e.target.value}:00`)}
                className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="break-start" className="block text-xs font-semibold text-text-primary mb-1">Break Start</label>
              <input
                id="break-start"
                type="time"
                value={breakStartTime.substring(0, 5)}
                onChange={(e) => setBreakStartTime(`${e.target.value}:00`)}
                className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
              />
            </div>
            <div>
              <label htmlFor="break-end" className="block text-xs font-semibold text-text-primary mb-1">Break End</label>
              <input
                id="break-end"
                type="time"
                value={breakEndTime.substring(0, 5)}
                onChange={(e) => setBreakEndTime(`${e.target.value}:00`)}
                className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
              />
            </div>
          </div>

          <div>
            <label htmlFor="slot-duration" className="block text-xs font-semibold text-text-primary mb-1">
              Slot Duration (Minutes)
            </label>
            <input
              id="slot-duration"
              type="number"
              min={10}
              max={60}
              step={5}
              value={slotDurationMinutes}
              onChange={(e) => setSlotDurationMinutes(Number(e.target.value))}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-surface-border">
            <Button variant="ghost" size="sm" onClick={() => setIsAddScheduleOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={addScheduleMutation.isPending}
              onClick={() =>
                addScheduleMutation.mutate({
                  dayOfWeek,
                  startTime,
                  endTime,
                  slotDurationMinutes,
                  breakStartTime,
                  breakEndTime,
                })
              }
            >
              Save Working Hours
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Add Exception Dialog */}
      <Dialog
        isOpen={isAddExceptionOpen}
        onClose={() => setIsAddExceptionOpen(false)}
        title="Record Doctor Leave / Schedule Exception"
      >
        <div className="space-y-4">
          <div>
            <label htmlFor="exception-date" className="block text-xs font-semibold text-text-primary mb-1">Exception Date</label>
            <input
              id="exception-date"
              type="date"
              min={new Date().toISOString().split('T')[0]}
              value={exceptionDate}
              onChange={(e) => setExceptionDate(e.target.value)}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            />
          </div>

          <div>
            <label htmlFor="exception-reason" className="block text-xs font-semibold text-text-primary mb-1">Reason / Note</label>
            <input
              id="exception-reason"
              type="text"
              placeholder="e.g. Conference attendance, personal leave..."
              value={exceptionReason}
              onChange={(e) => setExceptionReason(e.target.value)}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-surface-border">
            <Button variant="ghost" size="sm" onClick={() => setIsAddExceptionOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={addExceptionMutation.isPending}
              onClick={() =>
                addExceptionMutation.mutate({
                  exceptionDate,
                  isAvailable: false,
                  reason: exceptionReason,
                })
              }
            >
              Record Leave
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
};
