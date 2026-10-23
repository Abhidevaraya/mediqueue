import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { appointmentService } from '@/lib/api/appointmentService';
import { doctorService } from '@/lib/api/doctorService';
import { queueService } from '@/lib/api/queueService';
import { AppointmentResponse, AppointmentStatus } from '@/types/appointment';
import { SlotResponse } from '@/types/doctor';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { Dialog } from '@/components/ui/Dialog';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { useToast } from '@/lib/feedback/ToastContext';
import {
  Clock,
  Activity,
  RefreshCw,
  Plus,
} from 'lucide-react';

export const PatientAppointmentsPage: React.FC = () => {
  const toast = useToast();
  const queryClient = useQueryClient();

  const [selectedStatus, setSelectedStatus] = useState<AppointmentStatus | 'ALL'>('ALL');
  const [cancellingAppt, setCancellingAppt] = useState<AppointmentResponse | null>(null);
  const [cancelReason, setCancelReason] = useState<string>('');

  // Reschedule state
  const [reschedulingAppt, setReschedulingAppt] = useState<AppointmentResponse | null>(null);
  const [rescheduleDate, setRescheduleDate] = useState<string>('');
  const [selectedSlot, setSelectedSlot] = useState<SlotResponse | null>(null);

  // Appointments query
  const {
    data: pageData,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['appointments', 'my', selectedStatus],
    queryFn: () =>
      appointmentService.getMyAppointments(
        selectedStatus === 'ALL' ? undefined : selectedStatus,
        0,
        50
      ),
  });

  // Reschedule slots query
  const { data: availableSlots, isLoading: isSlotsLoading } = useQuery({
    queryKey: ['slots', reschedulingAppt?.doctorId, rescheduleDate],
    queryFn: () => doctorService.getAvailableSlots(reschedulingAppt!.doctorId, rescheduleDate),
    enabled: Boolean(reschedulingAppt?.doctorId) && Boolean(rescheduleDate),
  });

  // Check in mutation
  const checkInMutation = useMutation({
    mutationFn: queueService.checkInAppointment,
    onSuccess: (entry) => {
      toast.success(
        `Checked in successfully! Sequential queue token assigned: #${entry.tokenNumber}`,
        'Checked In'
      );
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
      queryClient.invalidateQueries({ queryKey: ['queue'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Check-in failed');
    },
  });

  // Cancel mutation
  const cancelMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      appointmentService.cancelAppointment(id, reason),
    onSuccess: () => {
      toast.info('Appointment has been cancelled and slot released', 'Appointment Cancelled');
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
      setCancellingAppt(null);
      setCancelReason('');
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to cancel appointment');
    },
  });

  // Reschedule mutation
  const rescheduleMutation = useMutation({
    mutationFn: ({ id, newStartDateTime }: { id: number; newStartDateTime: string }) =>
      appointmentService.rescheduleAppointment(id, newStartDateTime),
    onSuccess: (updated) => {
      toast.success(
        `Appointment rescheduled to ${new Date(updated.startDateTime).toLocaleDateString()}`,
        'Reschedule Confirmed'
      );
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
      setReschedulingAppt(null);
      setSelectedSlot(null);
      setRescheduleDate('');
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to reschedule appointment');
    },
  });

  const appointments = pageData?.content || [];

  const getStatusBadgeVariant = (status: AppointmentStatus) => {
    switch (status) {
      case 'BOOKED':
        return 'primary';
      case 'CHECKED_IN':
        return 'warning';
      case 'IN_PROGRESS':
        return 'primary';
      case 'COMPLETED':
        return 'success';
      case 'CANCELLED':
        return 'neutral';
      case 'NO_SHOW':
        return 'error';
      default:
        return 'neutral';
    }
  };

  const handleOpenReschedule = (appt: AppointmentResponse) => {
    setReschedulingAppt(appt);
    setSelectedSlot(null);
    const d = new Date();
    d.setDate(d.getDate() + 1);
    setRescheduleDate(d.toISOString().split('T')[0]);
  };

  const handleConfirmReschedule = () => {
    if (!reschedulingAppt || !selectedSlot) return;
    const newStartDateTime = `${selectedSlot.date}T${selectedSlot.startTime}`;
    rescheduleMutation.mutate({
      id: reschedulingAppt.id,
      newStartDateTime,
    });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <h1 className="text-2xl font-bold text-text-primary tracking-tight font-heading">
            My Appointments
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Review booked consultations, check in on visit day, or reschedule slots.
          </p>
        </div>
        <Link to="/patient/doctors">
          <Button variant="primary" size="md" leftIcon={<Plus className="w-4 h-4" />}>
            Book New Consultation
          </Button>
        </Link>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 text-xs">
        {(['ALL', 'BOOKED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'] as const).map(
          (status) => (
            <button
              key={status}
              onClick={() => setSelectedStatus(status)}
              className={`px-3 py-1.5 rounded-xl font-medium transition-colors whitespace-nowrap ${
                selectedStatus === status
                  ? 'bg-brand-lavender text-white font-semibold shadow-soft-sm'
                  : 'bg-surface-elevated text-text-secondary hover:text-text-primary border border-surface-border'
              }`}
            >
              {status === 'ALL' ? 'All Visits' : status.replace('_', ' ')}
            </button>
          )
        )}
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="p-16 flex flex-col items-center justify-center gap-3">
          <Spinner size="lg" className="text-brand-lavender" />
          <p className="text-sm font-medium text-text-secondary">Retrieving consultations...</p>
        </div>
      ) : isError ? (
        <ErrorState
          title="Could not load appointments"
          message="Failed to fetch your consultation history from the server."
          onRetry={() => refetch()}
        />
      ) : appointments.length === 0 ? (
        <EmptyState
          title="No appointments found"
          description={
            selectedStatus === 'ALL'
              ? 'You have not booked any hospital consultations yet.'
              : `No appointments found with status ${selectedStatus}.`
          }
        />
      ) : (
        <div className="space-y-4">
          {appointments.map((appt) => {
            const apptDate = new Date(appt.startDateTime);
            const isToday =
              new Date().toISOString().split('T')[0] === appt.startDateTime.split('T')[0];

            return (
              <Card key={appt.id} className="border-surface-border hover:shadow-soft-sm transition-shadow">
                <CardContent className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-5">
                  <div className="flex items-start gap-4">
                    <div className="w-14 h-14 rounded-2xl bg-brand-surface text-brand-lavender flex flex-col items-center justify-center shrink-0">
                      <span className="text-[10px] font-bold uppercase leading-none">
                        {apptDate.toLocaleDateString('en-US', { month: 'short' })}
                      </span>
                      <span className="text-lg font-extrabold leading-none mt-1">
                        {apptDate.getDate()}
                      </span>
                    </div>

                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h2 className="font-bold text-text-primary text-base truncate">{appt.doctorName}</h2>
                        <Badge variant={getStatusBadgeVariant(appt.status) as any} size="sm">
                          {appt.status}
                        </Badge>
                      </div>
                      <p className="text-xs text-text-secondary mt-0.5">
                        {appt.specialty} • {appt.departmentName}
                      </p>

                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-text-muted mt-2">
                        <span className="flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5" />
                          {apptDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} (
                          {appt.slotDurationMinutes} mins)
                        </span>
                        <span>Reference: #{appt.appointmentReference}</span>
                        <span>Fee: ${appt.consultationFee}</span>
                      </div>

                      {appt.reason && (
                        <p className="text-xs text-text-secondary mt-2 bg-surface-subtle p-2 rounded-lg">
                          Reason: {appt.reason}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Contextual Action Controls */}
                  <div className="flex flex-wrap items-center gap-2 self-end md:self-center shrink-0">
                    {/* Check In Action if today & BOOKED */}
                    {isToday && appt.status === 'BOOKED' && (
                      <Button
                        variant="primary"
                        size="sm"
                        isLoading={checkInMutation.isPending}
                        onClick={() => checkInMutation.mutate(appt.id)}
                      >
                        Check In Now
                      </Button>
                    )}

                    {/* View Live Queue if checked in or in progress */}
                    {(appt.status === 'CHECKED_IN' || appt.status === 'IN_PROGRESS') && (
                      <Link to="/patient/queue">
                        <Button variant="secondary" size="sm" leftIcon={<Activity className="w-4 h-4" />}>
                          Live Queue
                        </Button>
                      </Link>
                    )}

                    {/* Reschedule allowed if BOOKED */}
                    {appt.status === 'BOOKED' && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleOpenReschedule(appt)}
                        leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
                      >
                        Reschedule
                      </Button>
                    )}

                    {/* Cancel allowed if BOOKED or CHECKED_IN */}
                    {(appt.status === 'BOOKED' || appt.status === 'CHECKED_IN') && (
                      <Button
                        variant="danger"
                        size="sm"
                        onClick={() => setCancellingAppt(appt)}
                      >
                        Cancel
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Cancel Confirmation Dialog */}
      <ConfirmDialog
        isOpen={Boolean(cancellingAppt)}
        onClose={() => setCancellingAppt(null)}
        onConfirm={() => {
          if (cancellingAppt) {
            cancelMutation.mutate({ id: cancellingAppt.id, reason: cancelReason || undefined });
          }
        }}
        title="Cancel Appointment"
        message={`Are you sure you want to cancel your consultation with ${cancellingAppt?.doctorName}? This slot will be released for other patients.`}
        confirmLabel="Yes, Cancel Visit"
        variant="danger"
        isLoading={cancelMutation.isPending}
      />

      {/* Reschedule Modal */}
      {reschedulingAppt && (
        <Dialog
          isOpen={Boolean(reschedulingAppt)}
          onClose={() => setReschedulingAppt(null)}
          title="Reschedule Appointment"
        >
          <div className="space-y-4">
            <p className="text-xs text-text-secondary">
              Rescheduling consultation with <strong>{reschedulingAppt.doctorName}</strong>. Select a
              new date and available time slot:
            </p>

            <div>
              <label htmlFor="reschedule-date" className="block text-xs font-semibold text-text-primary mb-1">
                Select New Date
              </label>
              <input
                id="reschedule-date"
                type="date"
                min={new Date().toISOString().split('T')[0]}
                value={rescheduleDate}
                onChange={(e) => {
                  setRescheduleDate(e.target.value);
                  setSelectedSlot(null);
                }}
                className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-text-primary">Available Slots</label>
              {isSlotsLoading ? (
                <div className="p-4 text-center">
                  <Spinner size="sm" className="text-brand-lavender" />
                </div>
              ) : !availableSlots || availableSlots.length === 0 ? (
                <p className="text-xs text-text-muted p-3 bg-surface-subtle rounded-xl text-center">
                  No slots available on this date.
                </p>
              ) : (
                <div className="grid grid-cols-3 gap-2 max-h-40 overflow-y-auto p-1">
                  {availableSlots
                    .filter((s) => s.available)
                    .map((slot, idx) => (
                      <button
                        key={idx}
                        type="button"
                        onClick={() => setSelectedSlot(slot)}
                        className={`py-2 px-1 text-xs font-semibold rounded-lg border text-center transition-colors ${
                          selectedSlot?.startTime === slot.startTime
                            ? 'bg-brand-lavender text-white border-brand-lavender'
                            : 'bg-surface-elevated text-text-primary border-surface-border hover:bg-surface-subtle'
                        }`}
                      >
                        {slot.startTime.substring(0, 5)}
                      </button>
                    ))}
                </div>
              )}
            </div>

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-surface-border">
              <Button variant="ghost" size="sm" onClick={() => setReschedulingAppt(null)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                size="sm"
                disabled={!selectedSlot || rescheduleMutation.isPending}
                isLoading={rescheduleMutation.isPending}
                onClick={handleConfirmReschedule}
              >
                Confirm Reschedule
              </Button>
            </div>
          </div>
        </Dialog>
      )}
    </div>
  );
};
