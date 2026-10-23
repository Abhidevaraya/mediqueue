import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { clinicalService } from '@/lib/api/clinicalService';
import { appointmentService } from '@/lib/api/appointmentService';
import { ConsultationResponse, PrescriptionItemRequest } from '@/types/clinical';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { Dialog } from '@/components/ui/Dialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { useToast } from '@/lib/feedback/ToastContext';
import {
  FileText,
  Pill,
  Calendar,
  Plus,
  Trash2,
  Play,
  Check,
} from 'lucide-react';

export const DoctorConsultationWorkspacePage: React.FC = () => {
  const toast = useToast();
  const queryClient = useQueryClient();

  // Selected consultation for editing/authoring
  const [selectedConsultation, setSelectedConsultation] = useState<ConsultationResponse | null>(null);

  // Modal states
  const [isVisitSummaryOpen, setIsVisitSummaryOpen] = useState(false);
  const [isPrescriptionOpen, setIsPrescriptionOpen] = useState(false);
  const [isFollowUpOpen, setIsFollowUpOpen] = useState(false);

  // Visit summary draft form state
  const [chiefComplaint, setChiefComplaint] = useState('');
  const [diagnosis, setDiagnosis] = useState('');
  const [clinicalNotes, setClinicalNotes] = useState('');
  const [treatmentPlan, setTreatmentPlan] = useState('');

  // Prescription form state
  const [prescriptionNotes, setPrescriptionNotes] = useState('');
  const [prescriptionItems, setPrescriptionItems] = useState<PrescriptionItemRequest[]>([
    {
      medicationName: '',
      dosage: '',
      frequency: 'Once daily',
      durationDays: 7,
      instructions: 'Take after meals',
    },
  ]);

  // Follow-up form state
  const [followUpDate, setFollowUpDate] = useState(
    new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0]
  );
  const [followUpInstructions, setFollowUpInstructions] = useState('Routine review');

  // Queries
  const { data: consultationsPage, isLoading: isConsultationsLoading } = useQuery({
    queryKey: ['clinical', 'doctor-consultations'],
    queryFn: () => clinicalService.getDoctorConsultations(0, 50),
  });

  const { data: todayAppointments } = useQuery({
    queryKey: ['appointments', 'doctor', 'today'],
    queryFn: () => appointmentService.getDoctorTodayAppointments(),
  });

  // Start consultation mutation
  const startConsultMutation = useMutation({
    mutationFn: (appointmentId: number) => clinicalService.startConsultation(appointmentId),
    onSuccess: (consultation) => {
      toast.success(
        `Consultation started for ${consultation.patientName}`,
        'Consultation Session Active'
      );
      queryClient.invalidateQueries({ queryKey: ['clinical'] });
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
      setSelectedConsultation(consultation);
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to start consultation');
    },
  });

  // Complete consultation mutation
  const completeConsultMutation = useMutation({
    mutationFn: (consultationId: number) => clinicalService.completeConsultation(consultationId),
    onSuccess: () => {
      toast.success('Consultation session marked completed', 'Consultation Finished');
      queryClient.invalidateQueries({ queryKey: ['clinical'] });
      setSelectedConsultation(null);
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to complete consultation');
    },
  });

  // Visit summary save & approve mutation
  const saveSummaryMutation = useMutation({
    mutationFn: async () => {
      if (!selectedConsultation) return;
      const draft = await clinicalService.createVisitSummaryDraft({
        consultationId: selectedConsultation.id,
        chiefComplaint: chiefComplaint.trim() || undefined,
        diagnosis: diagnosis.trim() || undefined,
        clinicalNotes: clinicalNotes.trim() || undefined,
        treatmentPlan: treatmentPlan.trim() || undefined,
      });
      // Auto-approve draft to release to patient
      await clinicalService.approveVisitSummary(draft.id);
    },
    onSuccess: () => {
      toast.success('Clinical visit summary saved and approved for patient', 'Summary Approved');
      queryClient.invalidateQueries({ queryKey: ['clinical'] });
      setIsVisitSummaryOpen(false);
      setChiefComplaint('');
      setDiagnosis('');
      setClinicalNotes('');
      setTreatmentPlan('');
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to record visit summary');
    },
  });

  // Prescription issue mutation
  const issuePrescriptionMutation = useMutation({
    mutationFn: async () => {
      if (!selectedConsultation) return;
      const validItems = prescriptionItems.filter(
        (it) => it.medicationName.trim() && it.dosage.trim()
      );
      if (validItems.length === 0) {
        throw new Error('Please enter at least one medication with dosage');
      }
      return clinicalService.issuePrescription({
        consultationId: selectedConsultation.id,
        notes: prescriptionNotes.trim() || undefined,
        items: validItems,
      });
    },
    onSuccess: (rx) => {
      if (rx) {
        toast.success(
          `Digital prescription #${rx.prescriptionReference} issued successfully`,
          'Prescription Issued'
        );
      }
      queryClient.invalidateQueries({ queryKey: ['clinical'] });
      setIsPrescriptionOpen(false);
      setPrescriptionNotes('');
      setPrescriptionItems([
        {
          medicationName: '',
          dosage: '',
          frequency: 'Once daily',
          durationDays: 7,
          instructions: 'Take after meals',
        },
      ]);
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to issue prescription');
    },
  });

  // Follow-up create mutation
  const createFollowUpMutation = useMutation({
    mutationFn: () => {
      if (!selectedConsultation) throw new Error('No active consultation selected');
      return clinicalService.createFollowUp({
        consultationId: selectedConsultation.id,
        recommendedDate: followUpDate,
        instructions: followUpInstructions.trim() || undefined,
      });
    },
    onSuccess: (fu) => {
      toast.success(`Follow-up order created for ${fu.recommendedDate}`, 'Follow-up Scheduled');
      queryClient.invalidateQueries({ queryKey: ['clinical'] });
      setIsFollowUpOpen(false);
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to create follow-up order');
    },
  });

  const consultations = consultationsPage?.content || [];
  const inProgressList = consultations.filter((c) => c.status === 'IN_PROGRESS');

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <h1 className="text-2xl font-bold text-text-primary tracking-tight font-heading">
            Clinical Consultation Workspace
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Author clinical visit summaries, issue digital prescriptions, and order follow-up appointments.
          </p>
        </div>
      </div>

      {/* Active Consultation Banner */}
      {selectedConsultation ? (
        <Card className="border-status-success/40 bg-status-success-surface/20 shadow-soft-md">
          <CardContent className="p-6">
            <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <Badge variant="success" size="md">
                    IN PROGRESS
                  </Badge>
                  <span className="text-xs text-text-muted">
                    Ref #{selectedConsultation.consultationReference}
                  </span>
                </div>
                <h2 className="text-2xl font-extrabold text-text-primary font-heading">
                  Patient: {selectedConsultation.patientName}
                </h2>
                <p className="text-xs text-text-secondary">
                  Appointment Reference: #{selectedConsultation.appointmentReference}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2 shrink-0">
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => setIsVisitSummaryOpen(true)}
                  leftIcon={<FileText className="w-4 h-4" />}
                >
                  Visit Summary
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => setIsPrescriptionOpen(true)}
                  leftIcon={<Pill className="w-4 h-4" />}
                >
                  Write Prescription
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => setIsFollowUpOpen(true)}
                  leftIcon={<Calendar className="w-4 h-4" />}
                >
                  Order Follow-up
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  className="bg-status-success text-white hover:bg-status-success/90 border-transparent"
                  isLoading={completeConsultMutation.isPending}
                  onClick={() => completeConsultMutation.mutate(selectedConsultation.id)}
                  leftIcon={<Check className="w-4 h-4" />}
                >
                  Finish Consultation
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      ) : inProgressList.length > 0 ? (
        <Card className="border-brand-lavender/30 bg-surface-lavender/30">
          <CardContent className="p-4 flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-bold text-text-primary">
                Active Consultation: {inProgressList[0].patientName}
              </p>
              <p className="text-xs text-text-secondary">Click to resume authoring clinical notes.</p>
            </div>
            <Button
              variant="primary"
              size="sm"
              onClick={() => setSelectedConsultation(inProgressList[0])}
            >
              Resume Notes
            </Button>
          </CardContent>
        </Card>
      ) : null}

      {/* Today's Appointments Eligible to Start */}
      <div className="space-y-3">
        <h2 className="text-base font-bold text-text-primary tracking-tight font-heading">
          Today's Patient Consultations
        </h2>

        {!todayAppointments || todayAppointments.length === 0 ? (
          <p className="text-xs text-text-muted p-4 bg-surface-subtle rounded-xl text-center">
            No scheduled appointments today.
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {todayAppointments.map((appt) => (
              <Card key={appt.id} className="border-surface-border">
                <CardContent className="p-4 flex items-center justify-between gap-3 text-xs">
                  <div>
                    <span className="font-bold text-text-primary block">{appt.patientName}</span>
                    <span className="text-text-secondary">
                      {new Date(appt.startDateTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} •{' '}
                      {appt.status}
                    </span>
                  </div>

                  {(appt.status === 'BOOKED' ||
                    appt.status === 'CHECKED_IN' ||
                    appt.status === 'IN_PROGRESS') && (
                    <Button
                      variant="primary"
                      size="sm"
                      isLoading={startConsultMutation.isPending}
                      onClick={() => startConsultMutation.mutate(appt.id)}
                      leftIcon={<Play className="w-3.5 h-3.5" />}
                    >
                      Start
                    </Button>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Consultation History List */}
      <div className="space-y-4 pt-4 border-t border-surface-border">
        <h2 className="text-lg font-bold text-text-primary tracking-tight font-heading">
          Recent Consultations History
        </h2>

        {isConsultationsLoading ? (
          <div className="p-12 text-center">
            <Spinner size="md" className="text-brand-lavender mx-auto mb-2" />
            <p className="text-xs text-text-secondary">Loading consultation logs...</p>
          </div>
        ) : consultations.length === 0 ? (
          <EmptyState
            title="No consultation sessions recorded"
            description="Start a consultation on any scheduled appointment above to begin recording visit data."
          />
        ) : (
          <div className="space-y-3">
            {consultations.map((c) => (
              <Card key={c.id} className="border-surface-border">
                <CardContent className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-text-primary text-sm">{c.patientName}</h3>
                      <Badge
                        variant={c.status === 'COMPLETED' ? 'success' : c.status === 'IN_PROGRESS' ? 'primary' : 'neutral'}
                        size="sm"
                      >
                        {c.status}
                      </Badge>
                    </div>
                    <p className="text-text-secondary mt-0.5">
                      Ref #{c.consultationReference} • Appt #{c.appointmentReference}
                    </p>
                  </div>

                  <div className="flex items-center gap-2 self-end sm:self-center">
                    {c.status === 'IN_PROGRESS' && (
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={() => setSelectedConsultation(c)}
                      >
                        Open Workspace
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* DIALOG 1: VISIT SUMMARY */}
      <Dialog
        isOpen={isVisitSummaryOpen}
        onClose={() => setIsVisitSummaryOpen(false)}
        title="Author Clinical Visit Summary"
      >
        <div className="space-y-4">
          <div>
            <label htmlFor="chief-complaint" className="block text-xs font-semibold text-text-primary mb-1">
              Chief Complaint
            </label>
            <input
              id="chief-complaint"
              type="text"
              placeholder="e.g. Chest pain with shortness of breath..."
              value={chiefComplaint}
              onChange={(e) => setChiefComplaint(e.target.value)}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            />
          </div>

          <div>
            <label htmlFor="clinical-diagnosis" className="block text-xs font-semibold text-text-primary mb-1">
              Diagnosis
            </label>
            <textarea
              id="clinical-diagnosis"
              rows={2}
              placeholder="e.g. Mild exertional angina with sinus tachycardia..."
              value={diagnosis}
              onChange={(e) => setDiagnosis(e.target.value)}
              className="w-full p-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender resize-none"
            />
          </div>

          <div>
            <label htmlFor="clinical-notes" className="block text-xs font-semibold text-text-primary mb-1">
              Clinical Findings / Notes
            </label>
            <textarea
              id="clinical-notes"
              rows={3}
              placeholder="Detailed physical exam observations, vitals, lab analysis..."
              value={clinicalNotes}
              onChange={(e) => setClinicalNotes(e.target.value)}
              className="w-full p-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender resize-none"
            />
          </div>

          <div>
            <label htmlFor="treatment-plan" className="block text-xs font-semibold text-text-primary mb-1">
              Treatment Plan & Instructions
            </label>
            <textarea
              id="treatment-plan"
              rows={2}
              placeholder="Medication regimen, lifestyle recommendations, follow-up advice..."
              value={treatmentPlan}
              onChange={(e) => setTreatmentPlan(e.target.value)}
              className="w-full p-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender resize-none"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-surface-border">
            <Button variant="ghost" size="sm" onClick={() => setIsVisitSummaryOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={saveSummaryMutation.isPending}
              onClick={() => saveSummaryMutation.mutate()}
            >
              Approve & Release to Patient
            </Button>
          </div>
        </div>
      </Dialog>

      {/* DIALOG 2: DIGITAL PRESCRIPTION */}
      <Dialog
        isOpen={isPrescriptionOpen}
        onClose={() => setIsPrescriptionOpen(false)}
        title="Issue Digital Prescription"
      >
        <div className="space-y-4 max-h-[75vh] overflow-y-auto pr-1">
          <div>
            <label htmlFor="prescription-notes" className="block text-xs font-semibold text-text-primary mb-1">
              General Prescription Notes (Optional)
            </label>
            <input
              id="prescription-notes"
              type="text"
              placeholder="e.g. Complete full course of antibiotics..."
              value={prescriptionNotes}
              onChange={(e) => setPrescriptionNotes(e.target.value)}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            />
          </div>

          {/* Dynamic Medication Items */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-text-primary uppercase tracking-wider">
                Medications List
              </span>
              <Button
                variant="secondary"
                size="sm"
                className="h-8 text-xs"
                onClick={() =>
                  setPrescriptionItems([
                    ...prescriptionItems,
                    {
                      medicationName: '',
                      dosage: '',
                      frequency: 'Once daily',
                      durationDays: 7,
                      instructions: 'Take after meals',
                    },
                  ])
                }
                leftIcon={<Plus className="w-3.5 h-3.5" />}
              >
                Add Medication
              </Button>
            </div>

            {prescriptionItems.map((item, idx) => (
              <div
                key={idx}
                className="p-3.5 rounded-xl bg-surface-subtle border border-surface-border space-y-2 relative"
              >
                {prescriptionItems.length > 1 && (
                  <button
                    type="button"
                    onClick={() =>
                      setPrescriptionItems(prescriptionItems.filter((_, i) => i !== idx))
                    }
                    className="absolute right-2 top-2 p-1 text-status-error hover:bg-status-error-surface rounded-lg"
                    title="Remove item"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                )}

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label htmlFor={`med-name-${idx}`} className="block text-[11px] font-semibold text-text-secondary mb-0.5">
                      Medication Name
                    </label>
                    <input
                      id={`med-name-${idx}`}
                      type="text"
                      placeholder="e.g. Amoxicillin"
                      value={item.medicationName}
                      onChange={(e) => {
                        const copy = [...prescriptionItems];
                        copy[idx].medicationName = e.target.value;
                        setPrescriptionItems(copy);
                      }}
                      className="w-full h-9 px-2.5 rounded-lg text-xs bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
                    />
                  </div>
                  <div>
                    <label htmlFor={`med-dosage-${idx}`} className="block text-[11px] font-semibold text-text-secondary mb-0.5">
                      Dosage
                    </label>
                    <input
                      id={`med-dosage-${idx}`}
                      type="text"
                      placeholder="e.g. 500mg"
                      value={item.dosage}
                      onChange={(e) => {
                        const copy = [...prescriptionItems];
                        copy[idx].dosage = e.target.value;
                        setPrescriptionItems(copy);
                      }}
                      className="w-full h-9 px-2.5 rounded-lg text-xs bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-2">
                  <div>
                    <label htmlFor={`med-frequency-${idx}`} className="block text-[11px] font-semibold text-text-secondary mb-0.5">
                      Frequency
                    </label>
                    <input
                      id={`med-frequency-${idx}`}
                      type="text"
                      placeholder="e.g. Twice daily"
                      value={item.frequency}
                      onChange={(e) => {
                        const copy = [...prescriptionItems];
                        copy[idx].frequency = e.target.value;
                        setPrescriptionItems(copy);
                      }}
                      className="w-full h-9 px-2.5 rounded-lg text-xs bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
                    />
                  </div>
                  <div>
                    <label htmlFor={`med-duration-${idx}`} className="block text-[11px] font-semibold text-text-secondary mb-0.5">
                      Duration (Days)
                    </label>
                    <input
                      id={`med-duration-${idx}`}
                      type="number"
                      min={1}
                      value={item.durationDays}
                      onChange={(e) => {
                        const copy = [...prescriptionItems];
                        copy[idx].durationDays = Number(e.target.value);
                        setPrescriptionItems(copy);
                      }}
                      className="w-full h-9 px-2.5 rounded-lg text-xs bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
                    />
                  </div>
                  <div>
                    <label htmlFor={`med-instructions-${idx}`} className="block text-[11px] font-semibold text-text-secondary mb-0.5">
                      Instructions
                    </label>
                    <input
                      id={`med-instructions-${idx}`}
                      type="text"
                      placeholder="After food"
                      value={item.instructions || ''}
                      onChange={(e) => {
                        const copy = [...prescriptionItems];
                        copy[idx].instructions = e.target.value;
                        setPrescriptionItems(copy);
                      }}
                      className="w-full h-9 px-2.5 rounded-lg text-xs bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-surface-border">
            <Button variant="ghost" size="sm" onClick={() => setIsPrescriptionOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={issuePrescriptionMutation.isPending}
              onClick={() => issuePrescriptionMutation.mutate()}
            >
              Issue Digital Prescription
            </Button>
          </div>
        </div>
      </Dialog>

      {/* DIALOG 3: ORDER FOLLOW-UP */}
      <Dialog
        isOpen={isFollowUpOpen}
        onClose={() => setIsFollowUpOpen(false)}
        title="Order Clinical Follow-Up"
      >
        <div className="space-y-4">
          <div>
            <label htmlFor="followup-date" className="block text-xs font-semibold text-text-primary mb-1">
              Recommended Follow-Up Date
            </label>
            <input
              id="followup-date"
              type="date"
              min={new Date().toISOString().split('T')[0]}
              value={followUpDate}
              onChange={(e) => setFollowUpDate(e.target.value)}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            />
          </div>

          <div>
            <label htmlFor="followup-instructions" className="block text-xs font-semibold text-text-primary mb-1">
              Follow-Up Instructions
            </label>
            <input
              id="followup-instructions"
              type="text"
              placeholder="e.g. Return for BP check and repeat blood tests..."
              value={followUpInstructions}
              onChange={(e) => setFollowUpInstructions(e.target.value)}
              className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
            />
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-surface-border">
            <Button variant="ghost" size="sm" onClick={() => setIsFollowUpOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={createFollowUpMutation.isPending}
              onClick={() => createFollowUpMutation.mutate()}
            >
              Create Follow-Up Order
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
};
