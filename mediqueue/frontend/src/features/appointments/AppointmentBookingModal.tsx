import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Dialog } from '@/components/ui/Dialog';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { doctorService } from '@/lib/api/doctorService';
import { appointmentService } from '@/lib/api/appointmentService';
import { DoctorPublicResponse, SlotResponse } from '@/types/doctor';
import { useToast } from '@/lib/feedback/ToastContext';
import { AppApiError } from '@/lib/api/client';
import {
  Calendar,
  User,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

interface AppointmentBookingModalProps {
  doctor: DoctorPublicResponse | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const AppointmentBookingModal: React.FC<AppointmentBookingModalProps> = ({
  doctor,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const toast = useToast();
  const queryClient = useQueryClient();

  // Selected date defaults to tomorrow
  const getTomorrowDate = () => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  };

  const [selectedDate, setSelectedDate] = useState<string>(getTomorrowDate());
  const [selectedSlot, setSelectedSlot] = useState<SlotResponse | null>(null);
  const [reason, setReason] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Fetch available slots for doctor & date
  const {
    data: slots,
    isLoading: isSlotsLoading,
    refetch: refetchSlots,
  } = useQuery({
    queryKey: ['slots', doctor?.id, selectedDate],
    queryFn: () => doctorService.getAvailableSlots(doctor!.id, selectedDate),
    enabled: isOpen && Boolean(doctor?.id) && Boolean(selectedDate),
  });

  // Booking mutation
  const bookMutation = useMutation({
    mutationFn: appointmentService.bookAppointment,
    onSuccess: (appointment) => {
      toast.success(
        `Appointment booked successfully! Reference: ${appointment.appointmentReference}`,
        'Booking Confirmed'
      );
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
      queryClient.invalidateQueries({ queryKey: ['slots'] });
      if (onSuccess) onSuccess();
      handleClose();
    },
    onError: (err: any) => {
      if (err instanceof AppApiError) {
        setErrorMessage(err.message);
        toast.error(err.message, 'Booking Failed');
      } else {
        setErrorMessage('Unable to reserve slot. It may have just been booked by another patient.');
        toast.error('Booking conflict or server error');
      }
      refetchSlots();
    },
  });

  const handleClose = () => {
    setSelectedSlot(null);
    setReason('');
    setErrorMessage(null);
    onClose();
  };

  const handleConfirmBooking = () => {
    if (!doctor || !selectedSlot) return;
    setErrorMessage(null);

    // Format startDateTime as YYYY-MM-DDTHH:mm:ss
    const startDateTime = `${selectedSlot.date}T${selectedSlot.startTime}`;

    bookMutation.mutate({
      doctorId: doctor.id,
      startDateTime,
      reason: reason.trim() || undefined,
    });
  };

  if (!doctor) return null;

  return (
    <Dialog isOpen={isOpen} onClose={handleClose} title="Book Doctor Consultation">
      <div className="space-y-5">
        {/* Doctor Summary Banner */}
        <div className="p-4 rounded-xl bg-surface-lavender/40 border border-surface-border flex items-start gap-3.5">
          <div className="w-11 h-11 rounded-xl bg-brand-lavender text-white font-bold flex items-center justify-center shrink-0">
            <User className="w-5 h-5" />
          </div>
          <div className="min-w-0 flex-1">
            <h2 className="font-bold text-text-primary text-base leading-snug">{doctor.fullName}</h2>
            <p className="text-xs text-text-secondary mt-0.5">
              {doctor.specialty} • {doctor.departmentName}
            </p>
            <div className="mt-2 flex items-center gap-3 text-xs">
              <span className="text-text-muted">Fee: ${doctor.consultationFee}</span>
              <span className="text-text-muted">• {doctor.yearsOfExperience} yrs experience</span>
            </div>
          </div>
        </div>

        {errorMessage && (
          <div
            role="alert"
            className="p-3.5 rounded-xl bg-status-error-surface border border-status-error/30 text-status-error text-xs flex items-start gap-2.5"
          >
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <p className="leading-relaxed">{errorMessage}</p>
          </div>
        )}

        {/* Step 1: Select Consultation Date */}
        <div className="space-y-1.5">
          <label htmlFor="appointment-date" className="block text-xs font-semibold text-text-primary uppercase tracking-wider">
            1. Select Consultation Date
          </label>
          <div className="relative">
            <input
              id="appointment-date"
              type="date"
              min={new Date().toISOString().split('T')[0]}
              value={selectedDate}
              onChange={(e) => {
                setSelectedDate(e.target.value);
                setSelectedSlot(null);
                setErrorMessage(null);
              }}
              className="w-full h-11 px-4 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender focus:ring-2 focus:ring-brand-lavender/20"
            />
          </div>
        </div>

        {/* Step 2: Available Slots Grid */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <label className="block text-xs font-semibold text-text-primary uppercase tracking-wider">
              2. Choose Available Time Slot
            </label>
            <span className="text-xs text-text-muted">
              {slots?.filter((s) => s.available).length ?? 0} slots available
            </span>
          </div>

          {isSlotsLoading ? (
            <div className="p-8 text-center bg-surface-subtle rounded-xl flex flex-col items-center gap-2">
              <Spinner size="md" className="text-brand-lavender" />
              <p className="text-xs text-text-secondary">Retrieving live doctor availability...</p>
            </div>
          ) : !slots || slots.length === 0 ? (
            <div className="p-6 text-center bg-surface-subtle rounded-xl border border-dashed border-surface-border">
              <Calendar className="w-8 h-8 text-text-muted mx-auto mb-2 opacity-50" />
              <p className="text-sm font-medium text-text-primary">No slots scheduled for this date</p>
              <p className="text-xs text-text-secondary mt-1">
                The doctor has no published working hours or exceptions for the selected day.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 max-h-48 overflow-y-auto p-1">
              {slots.map((slot, idx) => {
                const isSelected =
                  selectedSlot?.startTime === slot.startTime && selectedSlot?.date === slot.date;
                const timeLabel = slot.startTime.substring(0, 5);

                if (!slot.available) {
                  return (
                    <button
                      key={idx}
                      type="button"
                      disabled
                      className="py-2.5 px-2 rounded-xl text-xs font-medium bg-surface-subtle text-text-muted/50 border border-transparent cursor-not-allowed line-through text-center"
                      title={slot.unavailabilityReason || 'Slot already booked'}
                    >
                      {timeLabel}
                    </button>
                  );
                }

                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      setSelectedSlot(slot);
                      setErrorMessage(null);
                    }}
                    className={`py-2.5 px-2 rounded-xl text-xs font-semibold transition-all duration-200 border text-center ${
                      isSelected
                        ? 'bg-brand-lavender text-white border-brand-lavender shadow-soft-sm scale-[1.02]'
                        : 'bg-surface-elevated text-text-primary border-surface-border hover:border-brand-lavender/50 hover:bg-surface-subtle'
                    }`}
                  >
                    {timeLabel}
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Step 3: Optional Reason / Notes */}
        <div className="space-y-1.5">
          <label htmlFor="appointment-reason" className="block text-xs font-semibold text-text-primary uppercase tracking-wider">
            3. Reason for Consultation (Optional)
          </label>
          <textarea
            id="appointment-reason"
            rows={2}
            placeholder="Briefly describe your symptoms or reason for visit..."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full p-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender focus:ring-2 focus:ring-brand-lavender/20 resize-none"
          />
        </div>

        {/* Selected Summary Card */}
        {selectedSlot && (
          <div className="p-3.5 rounded-xl bg-status-success-surface border border-status-success/20 text-xs text-status-success flex items-center justify-between">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>
                Selected: <strong>{selectedSlot.date}</strong> at{' '}
                <strong>{selectedSlot.startTime.substring(0, 5)}</strong> ({selectedSlot.durationMinutes} mins)
              </span>
            </div>
            <Badge variant="success" size="sm">
              Available
            </Badge>
          </div>
        )}

        {/* Dialog Actions */}
        <div className="flex items-center justify-end gap-3 pt-3 border-t border-surface-border">
          <Button variant="ghost" size="md" onClick={handleClose} disabled={bookMutation.isPending}>
            Cancel
          </Button>
          <Button
            variant="primary"
            size="md"
            disabled={!selectedSlot || bookMutation.isPending}
            isLoading={bookMutation.isPending}
            onClick={handleConfirmBooking}
            leftIcon={<Calendar className="w-4 h-4" />}
          >
            Confirm Booking
          </Button>
        </div>
      </div>
    </Dialog>
  );
};
