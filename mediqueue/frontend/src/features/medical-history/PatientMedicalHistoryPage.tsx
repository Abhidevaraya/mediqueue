import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { clinicalService } from '@/lib/api/clinicalService';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { useToast } from '@/lib/feedback/ToastContext';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  FileText,
  Pill,
  Calendar,
  Download,
  Upload,
  ShieldCheck,
} from 'lucide-react';
import { DocumentCategory } from '@/types/clinical';

export const PatientMedicalHistoryPage: React.FC = () => {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<'summaries' | 'prescriptions' | 'followups' | 'documents'>('summaries');

  // Queries
  const { data: summariesPage, isLoading: isSummariesLoading } = useQuery({
    queryKey: ['clinical', 'summaries'],
    queryFn: () => clinicalService.getMyMedicalRecords(0, 50),
    enabled: activeTab === 'summaries',
  });

  const { data: prescriptionsPage, isLoading: isPrescriptionsLoading } = useQuery({
    queryKey: ['clinical', 'prescriptions'],
    queryFn: () => clinicalService.getMyPrescriptions(0, 50),
    enabled: activeTab === 'prescriptions',
  });

  const { data: followUpsPage, isLoading: isFollowUpsLoading } = useQuery({
    queryKey: ['clinical', 'followups'],
    queryFn: () => clinicalService.getMyFollowUps(0, 50),
    enabled: activeTab === 'followups',
  });

  const { data: documentsPage, isLoading: isDocumentsLoading } = useQuery({
    queryKey: ['clinical', 'documents'],
    queryFn: () => clinicalService.getMyMedicalDocuments(0, 50),
    enabled: activeTab === 'documents',
  });

  // Upload document mutation
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<DocumentCategory>('LAB_REPORT');

  const uploadMutation = useMutation({
    mutationFn: () =>
      clinicalService.uploadMedicalDocument(user!.id, selectedFile!, { category: selectedCategory }),
    onSuccess: () => {
      toast.success('Medical document uploaded successfully', 'Document Uploaded');
      queryClient.invalidateQueries({ queryKey: ['clinical', 'documents'] });
      setSelectedFile(null);
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Failed to upload document');
    },
  });

  // Download document handler
  const handleDownload = async (docId: number, filename: string) => {
    try {
      toast.info(`Preparing secure download for ${filename}...`);
      await clinicalService.downloadMedicalDocument(docId, filename);
    } catch {
      toast.error('Failed to download document');
    }
  };

  const summaries = summariesPage?.content || [];
  const prescriptions = prescriptionsPage?.content || [];
  const followUps = followUpsPage?.content || [];
  const documents = documentsPage?.content || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-4 border-b border-surface-border">
        <h1 className="text-2xl font-bold text-text-primary tracking-tight font-heading">
          Clinical History & Medical Records
        </h1>
        <p className="text-sm text-text-secondary mt-1">
          Review clinician-approved visit summaries, digital prescriptions, follow-up instructions, and secure documents.
        </p>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-surface-border pb-1">
        <button
          onClick={() => setActiveTab('summaries')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ${
            activeTab === 'summaries'
              ? 'bg-brand-lavender text-white shadow-soft-sm'
              : 'text-text-secondary hover:text-text-primary hover:bg-surface-subtle'
          }`}
        >
          <FileText className="w-4 h-4" />
          <span>Visit Summaries ({summaries.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('prescriptions')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ${
            activeTab === 'prescriptions'
              ? 'bg-brand-lavender text-white shadow-soft-sm'
              : 'text-text-secondary hover:text-text-primary hover:bg-surface-subtle'
          }`}
        >
          <Pill className="w-4 h-4" />
          <span>Prescriptions ({prescriptions.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('followups')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ${
            activeTab === 'followups'
              ? 'bg-brand-lavender text-white shadow-soft-sm'
              : 'text-text-secondary hover:text-text-primary hover:bg-surface-subtle'
          }`}
        >
          <Calendar className="w-4 h-4" />
          <span>Follow-ups ({followUps.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('documents')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all ${
            activeTab === 'documents'
              ? 'bg-brand-lavender text-white shadow-soft-sm'
              : 'text-text-secondary hover:text-text-primary hover:bg-surface-subtle'
          }`}
        >
          <Upload className="w-4 h-4" />
          <span>Medical Documents ({documents.length})</span>
        </button>
      </div>

      {/* TAB 1: VISIT SUMMARIES */}
      {activeTab === 'summaries' && (
        <div className="space-y-4">
          {isSummariesLoading ? (
            <div className="p-16 text-center">
              <Spinner size="lg" className="text-brand-lavender mx-auto mb-2" />
              <p className="text-sm text-text-secondary">Loading visit summaries...</p>
            </div>
          ) : summaries.length === 0 ? (
            <EmptyState
              title="No visit summaries found"
              description="Clinical visit summaries are published and approved by your attending clinician following consultations."
            />
          ) : (
            summaries.map((summary) => (
              <Card key={summary.id} className="border-surface-border">
                <CardHeader>
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <CardTitle className="text-base">{summary.chiefComplaint || 'Consultation Visit'}</CardTitle>
                        <Badge variant="success" size="sm">
                          {summary.status}
                        </Badge>
                      </div>
                      <p className="text-xs text-text-secondary mt-0.5">
                        Authoring Physician: {summary.authoringDoctorName} • Ref #{summary.summaryReference}
                      </p>
                    </div>
                    {summary.approvedAt && (
                      <div className="flex items-center gap-1.5 text-xs text-text-muted">
                        <ShieldCheck className="w-4 h-4 text-status-success" />
                        <span>Approved {new Date(summary.approvedAt).toLocaleDateString()}</span>
                      </div>
                    )}
                  </div>
                </CardHeader>
                <CardContent className="space-y-4 text-xs">
                  {summary.diagnosis && (
                    <div className="p-3.5 rounded-xl bg-surface-lavender/30 border border-surface-border">
                      <span className="font-bold text-text-primary block mb-1">Clinical Diagnosis</span>
                      <p className="text-text-secondary leading-relaxed">{summary.diagnosis}</p>
                    </div>
                  )}

                  {summary.clinicalNotes && (
                    <div>
                      <span className="font-bold text-text-primary block mb-1">Physician Notes</span>
                      <p className="text-text-secondary leading-relaxed bg-surface-subtle p-3 rounded-xl">
                        {summary.clinicalNotes}
                      </p>
                    </div>
                  )}

                  {summary.treatmentPlan && (
                    <div className="p-3.5 rounded-xl bg-status-success-surface border border-status-success/20">
                      <span className="font-bold text-status-success block mb-1">Treatment Plan & Guidance</span>
                      <p className="text-text-secondary leading-relaxed">{summary.treatmentPlan}</p>
                    </div>
                  )}
                </CardContent>
              </Card>
            ))
          )}
        </div>
      )}

      {/* TAB 2: PRESCRIPTIONS */}
      {activeTab === 'prescriptions' && (
        <div className="space-y-4">
          {isPrescriptionsLoading ? (
            <div className="p-16 text-center">
              <Spinner size="lg" className="text-brand-lavender mx-auto mb-2" />
              <p className="text-sm text-text-secondary">Loading prescriptions...</p>
            </div>
          ) : prescriptions.length === 0 ? (
            <EmptyState
              title="No prescriptions found"
              description="Digital e-prescriptions issued by physicians during consultations will be stored here."
            />
          ) : (
            prescriptions.map((rx) => (
              <Card key={rx.id} className="border-surface-border">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <div>
                      <CardTitle className="text-base flex items-center gap-2">
                        <Pill className="w-5 h-5 text-brand-lavender" />
                        Prescription #{rx.prescriptionReference}
                      </CardTitle>
                      <p className="text-xs text-text-secondary mt-0.5">
                        Prescribing Doctor: {rx.doctorName} • Issued {new Date(rx.issuedAt).toLocaleDateString()}
                      </p>
                    </div>
                    <Badge variant={rx.status === 'ISSUED' ? 'success' : 'neutral'} size="sm">
                      {rx.status}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {rx.notes && (
                    <p className="text-xs text-text-secondary bg-surface-subtle p-2.5 rounded-lg italic">
                      Notes: {rx.notes}
                    </p>
                  )}

                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                      <thead>
                        <tr className="border-b border-surface-border text-text-muted font-semibold">
                          <th className="pb-2">Medication</th>
                          <th className="pb-2">Dosage</th>
                          <th className="pb-2">Frequency</th>
                          <th className="pb-2">Duration</th>
                          <th className="pb-2">Instructions</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-surface-border/60">
                        {rx.items.map((item, idx) => (
                          <tr key={idx} className="hover:bg-surface-subtle/50">
                            <td className="py-2.5 font-bold text-text-primary">{item.medicationName}</td>
                            <td className="py-2.5 text-text-secondary">{item.dosage}</td>
                            <td className="py-2.5 text-text-secondary">{item.frequency}</td>
                            <td className="py-2.5 text-text-secondary">{item.durationDays} days</td>
                            <td className="py-2.5 text-text-secondary">{item.instructions || 'As directed'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </div>
      )}

      {/* TAB 3: FOLLOW-UPS */}
      {activeTab === 'followups' && (
        <div className="space-y-4">
          {isFollowUpsLoading ? (
            <div className="p-16 text-center">
              <Spinner size="lg" className="text-brand-lavender mx-auto mb-2" />
              <p className="text-sm text-text-secondary">Loading follow-ups...</p>
            </div>
          ) : followUps.length === 0 ? (
            <EmptyState
              title="No follow-up recommendations"
              description="No clinical follow-up dates have been ordered for your visits."
            />
          ) : (
            followUps.map((fu) => (
              <Card key={fu.id} className="border-surface-border">
                <CardContent className="p-5 flex items-start justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h2 className="font-bold text-text-primary text-sm">
                        Recommended Date: {fu.recommendedDate}
                      </h2>
                      <Badge variant="primary" size="sm">
                        {fu.status}
                      </Badge>
                    </div>
                    <p className="text-xs text-text-secondary">
                      Ordered by {fu.doctorName} (Ref #{fu.followUpReference})
                    </p>
                    {fu.instructions && (
                      <p className="text-xs text-text-primary bg-surface-subtle p-2.5 rounded-lg mt-2">
                        Instructions: {fu.instructions}
                      </p>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </div>
      )}

      {/* TAB 4: MEDICAL DOCUMENTS */}
      {activeTab === 'documents' && (
        <div className="space-y-6">
          {/* Upload Card */}
          <Card className="border-surface-border">
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Upload className="w-5 h-5 text-brand-lavender" />
                Upload Secure Medical Document
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-1">
                  <label htmlFor="document-category" className="block text-xs font-semibold text-text-primary mb-1">
                    Document Category
                  </label>
                  <select
                    id="document-category"
                    value={selectedCategory}
                    onChange={(e) => setSelectedCategory(e.target.value as DocumentCategory)}
                    className="w-full h-10 px-3 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender"
                  >
                    <option value="LAB_REPORT">Lab Report</option>
                    <option value="IMAGING">Imaging / X-Ray</option>
                    <option value="DISCHARGE_SUMMARY">Discharge Summary</option>
                    <option value="PRESCRIPTION_SCAN">Prescription Scan</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>

                <div className="sm:col-span-1">
                  <label htmlFor="document-file" className="block text-xs font-semibold text-text-primary mb-1">
                    Select File (PDF, PNG, JPEG)
                  </label>
                  <input
                    id="document-file"
                    type="file"
                    accept=".pdf,.png,.jpg,.jpeg"
                    onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
                    className="w-full text-xs text-text-secondary file:mr-3 file:py-2 file:px-3 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-brand-surface file:text-brand-lavender hover:file:bg-brand-surface/80"
                  />
                </div>

                <div className="sm:col-span-1 flex items-end">
                  <Button
                    variant="primary"
                    size="md"
                    className="w-full"
                    disabled={!selectedFile || uploadMutation.isPending}
                    isLoading={uploadMutation.isPending}
                    onClick={() => uploadMutation.mutate()}
                  >
                    Upload Document
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Document List */}
          {isDocumentsLoading ? (
            <div className="p-16 text-center">
              <Spinner size="lg" className="text-brand-lavender mx-auto mb-2" />
              <p className="text-sm text-text-secondary">Loading medical documents...</p>
            </div>
          ) : documents.length === 0 ? (
            <EmptyState
              title="No documents uploaded"
              description="Upload lab reports, discharge summaries, or imaging scans to keep your health records consolidated."
            />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {documents.map((doc) => (
                <Card key={doc.id} className="border-surface-border hover:shadow-soft-sm transition-shadow">
                  <CardContent className="p-4 flex items-center justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 truncate">
                        <FileText className="w-5 h-5 text-brand-lavender shrink-0" />
                        <h2 className="font-bold text-text-primary text-sm truncate">{doc.originalFileName}</h2>
                      </div>
                      <div className="flex items-center gap-2 text-xs text-text-muted mt-1">
                        <Badge variant="primary" size="sm">
                          {doc.documentCategory.replace('_', ' ')}
                        </Badge>
                        <span>{(doc.fileSizeBytes / 1024).toFixed(1)} KB</span>
                        <span>•</span>
                        <span>{new Date(doc.createdAt).toLocaleDateString()}</span>
                      </div>
                    </div>

                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => handleDownload(doc.id, doc.originalFileName)}
                      leftIcon={<Download className="w-4 h-4" />}
                    >
                      Download
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
