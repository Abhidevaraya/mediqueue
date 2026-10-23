import React, { useState, useEffect, useCallback } from 'react';
import { adminService } from '@/lib/api/adminService';
import {
  DoctorAdminResponse,
  DoctorVerificationStatus,
  AccountStatus,
  DoctorCreateRequest,
  DoctorUpdateRequest,
} from '@/types/admin';
import { DepartmentResponse } from '@/types/doctor';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { Dialog } from '@/components/ui/Dialog';
import {
  UserPlus,
  CheckCircle,
  XCircle,
  Edit2,
  RefreshCw,
  ShieldCheck,
  AlertCircle,
} from 'lucide-react';

export const AdminDoctorsPage: React.FC = () => {
  const [doctors, setDoctors] = useState<DoctorAdminResponse[]>([]);
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [filterVerification, setFilterVerification] = useState<DoctorVerificationStatus | ''>('');
  const [filterDepartmentId, setFilterDepartmentId] = useState<number | ''>('');

  // Modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingDoctor, setEditingDoctor] = useState<DoctorAdminResponse | null>(null);
  const [verificationDoctor, setVerificationDoctor] = useState<DoctorAdminResponse | null>(null);
  const [newVerificationStatus, setNewVerificationStatus] = useState<DoctorVerificationStatus>('VERIFIED');
  const [verificationReason, setVerificationReason] = useState('');

  // Form States
  const [createForm, setCreateForm] = useState<DoctorCreateRequest>({
    fullName: '',
    email: '',
    password: '',
    phoneNumber: '',
    departmentId: 0,
    specialty: '',
    qualification: '',
    yearsOfExperience: 0,
    consultationFee: 50.0,
    professionalBio: '',
  });

  const [editForm, setEditForm] = useState<DoctorUpdateRequest>({});

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [deptList, docPage] = await Promise.all([
        adminService.getDepartments(),
        adminService.getDoctors({
          verificationStatus: (filterVerification as DoctorVerificationStatus) || undefined,
          departmentId: filterDepartmentId !== '' ? Number(filterDepartmentId) : undefined,
          page: 0,
          size: 50,
        }),
      ]);
      const departmentsArray = Array.isArray(deptList) ? deptList : [];
      setDepartments(departmentsArray);
      const doctorsArray = docPage && 'content' in docPage && Array.isArray(docPage.content)
        ? docPage.content
        : Array.isArray(docPage)
        ? docPage
        : [];
      setDoctors(doctorsArray);
      if (departmentsArray.length > 0 && createForm.departmentId === 0) {
        setCreateForm((prev) => ({ ...prev, departmentId: departmentsArray[0].id }));
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load doctors and departments';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [filterVerification, filterDepartmentId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreateDoctor = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.fullName.trim() || !createForm.email.trim() || !createForm.specialty.trim() || !createForm.departmentId) {
      setFormError('Please fill out all required fields marked with *.');
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      const created = await adminService.createDoctor(createForm);
      setDoctors((prev) => [created, ...prev]);
      setShowCreateModal(false);
      setCreateForm({
        fullName: '',
        email: '',
        password: '',
        phoneNumber: '',
        departmentId: departments[0]?.id || 0,
        specialty: '',
        qualification: '',
        yearsOfExperience: 0,
        consultationFee: 50.0,
        professionalBio: '',
      });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to onboard doctor';
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditDoctor = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingDoctor) return;
    setSubmitting(true);
    setFormError(null);
    try {
      const updated = await adminService.updateDoctor(editingDoctor.id, editForm);
      setDoctors((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
      setEditingDoctor(null);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to update doctor profile';
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerificationSubmit = async () => {
    if (!verificationDoctor) return;
    setSubmitting(true);
    setFormError(null);
    try {
      const updated = await adminService.updateDoctorVerification(
        verificationDoctor.id,
        newVerificationStatus,
        verificationReason.trim() || undefined
      );
      setDoctors((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
      setVerificationDoctor(null);
      setVerificationReason('');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to update verification status';
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleDoctorStatus = async (doctor: DoctorAdminResponse) => {
    const targetStatus: AccountStatus = doctor.accountStatus === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
    try {
      const updated = await adminService.updateDoctorStatus(doctor.id, targetStatus);
      setDoctors((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to update doctor account status');
    }
  };

  const getVerificationBadge = (status: DoctorVerificationStatus) => {
    switch (status) {
      case 'VERIFIED':
        return <Badge variant="success">Verified</Badge>;
      case 'REJECTED':
        return <Badge variant="error">Rejected</Badge>;
      case 'PENDING':
      default:
        return <Badge variant="warning">Pending</Badge>;
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-text-primary tracking-tight">
            Doctor Management & Credentialing
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Onboard clinical specialists, review credentials, assign departments, and maintain active licenses.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={fetchData}>
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Refresh
          </Button>
          <Button variant="primary" size="sm" onClick={() => setShowCreateModal(true)}>
            <UserPlus className="w-4 h-4 mr-1.5" />
            Onboard Doctor
          </Button>
        </div>
      </div>

      {/* Filter Bar */}
      <Card className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Verification Status
            </label>
            <select
              value={filterVerification}
              onChange={(e) => setFilterVerification(e.target.value as DoctorVerificationStatus | '')}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            >
              <option value="">All Verification States</option>
              <option value="VERIFIED">Verified</option>
              <option value="PENDING">Pending Review</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Department Filter
            </label>
            <select
              value={filterDepartmentId}
              onChange={(e) => setFilterDepartmentId(e.target.value ? Number(e.target.value) : '')}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            >
              <option value="">All Departments</option>
              {departments.map((dept) => (
                <option key={dept.id} value={dept.id}>
                  {dept.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-end justify-end text-xs text-text-muted pb-2">
            <span>{doctors.length} doctors found</span>
          </div>
        </div>
      </Card>

      {/* Main Table */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <Spinner size="lg" />
          <p className="text-sm font-medium text-text-secondary">Loading doctor roster...</p>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Doctors"
          message={error}
          onRetry={fetchData}
        />
      ) : doctors.length === 0 ? (
        <EmptyState
          title="No doctors found"
          description="Click 'Onboard Doctor' to create a doctor account or adjust your filters."
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-surface-border bg-surface-subtle/50 text-xs font-semibold text-text-muted uppercase tracking-wider">
                  <th className="px-5 py-3">Doctor</th>
                  <th className="px-5 py-3">Department & Specialty</th>
                  <th className="px-5 py-3">Qualifications & Exp</th>
                  <th className="px-5 py-3">Consult Fee</th>
                  <th className="px-5 py-3">Verification</th>
                  <th className="px-5 py-3">Account</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-border">
                {doctors.map((doc) => (
                  <tr key={doc.id} className="hover:bg-surface-subtle/40 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="font-medium text-text-primary">{doc.fullName}</div>
                      <div className="text-xs text-text-secondary">{doc.email}</div>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="font-semibold text-text-primary text-xs">{doc.departmentName}</div>
                      <div className="text-xs text-brand-lavender">{doc.specialty}</div>
                    </td>
                    <td className="px-5 py-3.5 text-xs text-text-secondary">
                      <div>{doc.qualification}</div>
                      <div className="text-text-muted">{doc.yearsOfExperience} yrs experience</div>
                    </td>
                    <td className="px-5 py-3.5 font-mono text-sm text-text-primary">
                      ${Number(doc.consultationFee).toFixed(2)}
                    </td>
                    <td className="px-5 py-3.5">
                      {getVerificationBadge(doc.verificationStatus)}
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge variant={doc.accountStatus === 'ACTIVE' ? 'success' : 'error'}>
                        {doc.accountStatus}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setVerificationDoctor(doc);
                            setNewVerificationStatus(doc.verificationStatus === 'VERIFIED' ? 'REJECTED' : 'VERIFIED');
                          }}
                          title="Verify or Reject Credentials"
                        >
                          <ShieldCheck className="w-4 h-4 text-brand-lavender" />
                        </Button>

                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setEditingDoctor(doc);
                            setEditForm({
                              specialty: doc.specialty,
                              qualification: doc.qualification,
                              yearsOfExperience: doc.yearsOfExperience,
                              consultationFee: doc.consultationFee,
                              professionalBio: doc.professionalBio || '',
                              departmentId: doc.departmentId,
                            });
                          }}
                          title="Edit Doctor Details"
                        >
                          <Edit2 className="w-4 h-4 text-text-muted" />
                        </Button>

                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => toggleDoctorStatus(doc)}
                          title={doc.accountStatus === 'ACTIVE' ? 'Block Doctor' : 'Activate Doctor'}
                        >
                          {doc.accountStatus === 'ACTIVE' ? (
                            <XCircle className="w-4 h-4 text-state-error" />
                          ) : (
                            <CheckCircle className="w-4 h-4 text-state-success" />
                          )}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Onboard Doctor Modal */}
      <Dialog
        isOpen={showCreateModal}
        onClose={() => {
          setShowCreateModal(false);
          setFormError(null);
        }}
        title="Onboard Medical Specialist"
      >
        <form onSubmit={handleCreateDoctor} className="space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Full Name *
              </label>
              <input
                type="text"
                required
                value={createForm.fullName}
                onChange={(e) => setCreateForm({ ...createForm, fullName: e.target.value })}
                placeholder="Dr. Gregory House"
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Email Address *
              </label>
              <input
                type="email"
                required
                value={createForm.email}
                onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                placeholder="doctor@hospital.org"
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Temporary Password
              </label>
              <input
                type="password"
                value={createForm.password}
                onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                placeholder="Defaults to SecurePass123! if blank"
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Phone Number
              </label>
              <input
                type="tel"
                value={createForm.phoneNumber}
                onChange={(e) => setCreateForm({ ...createForm, phoneNumber: e.target.value })}
                placeholder="+1-555-0199"
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Department *
              </label>
              <select
                required
                value={createForm.departmentId}
                onChange={(e) => setCreateForm({ ...createForm, departmentId: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              >
                <option value={0}>Select Department</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Specialty *
              </label>
              <input
                type="text"
                required
                value={createForm.specialty}
                onChange={(e) => setCreateForm({ ...createForm, specialty: e.target.value })}
                placeholder="Cardiology, Diagnostics, etc."
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Qualification
              </label>
              <input
                type="text"
                value={createForm.qualification}
                onChange={(e) => setCreateForm({ ...createForm, qualification: e.target.value })}
                placeholder="MD, MBBS, FACC"
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Experience (Years)
              </label>
              <input
                type="number"
                min={0}
                value={createForm.yearsOfExperience}
                onChange={(e) => setCreateForm({ ...createForm, yearsOfExperience: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Fee ($)
              </label>
              <input
                type="number"
                step="0.01"
                min={0}
                value={createForm.consultationFee}
                onChange={(e) => setCreateForm({ ...createForm, consultationFee: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Professional Biography
            </label>
            <textarea
              rows={2}
              value={createForm.professionalBio}
              onChange={(e) => setCreateForm({ ...createForm, professionalBio: e.target.value })}
              placeholder="Summary of clinical background and affiliations..."
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>

          {formError && (
            <div className="p-3 rounded-xl bg-state-error/10 border border-state-error/20 text-state-error text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div className="flex justify-end gap-2 pt-3">
            <Button
              variant="secondary"
              type="button"
              onClick={() => setShowCreateModal(false)}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={submitting}>
              Onboard Doctor
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Edit Doctor Modal */}
      <Dialog
        isOpen={Boolean(editingDoctor)}
        onClose={() => {
          setEditingDoctor(null);
          setFormError(null);
        }}
        title={`Edit Doctor Profile: ${editingDoctor?.fullName}`}
      >
        <form onSubmit={handleEditDoctor} className="space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Specialty
              </label>
              <input
                type="text"
                value={editForm.specialty || ''}
                onChange={(e) => setEditForm({ ...editForm, specialty: e.target.value })}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Department
              </label>
              <select
                value={editForm.departmentId || editingDoctor?.departmentId}
                onChange={(e) => setEditForm({ ...editForm, departmentId: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              >
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Qualification
              </label>
              <input
                type="text"
                value={editForm.qualification || ''}
                onChange={(e) => setEditForm({ ...editForm, qualification: e.target.value })}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Experience (Years)
              </label>
              <input
                type="number"
                min={0}
                value={editForm.yearsOfExperience || 0}
                onChange={(e) => setEditForm({ ...editForm, yearsOfExperience: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Fee ($)
              </label>
              <input
                type="number"
                step="0.01"
                min={0}
                value={editForm.consultationFee || 0}
                onChange={(e) => setEditForm({ ...editForm, consultationFee: Number(e.target.value) })}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Professional Biography
            </label>
            <textarea
              rows={3}
              value={editForm.professionalBio || ''}
              onChange={(e) => setEditForm({ ...editForm, professionalBio: e.target.value })}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>

          {formError && (
            <div className="p-3 rounded-xl bg-state-error/10 border border-state-error/20 text-state-error text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div className="flex justify-end gap-2 pt-3">
            <Button
              variant="secondary"
              type="button"
              onClick={() => setEditingDoctor(null)}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={submitting}>
              Save Changes
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Verification Dialog */}
      <Dialog
        isOpen={Boolean(verificationDoctor)}
        onClose={() => {
          setVerificationDoctor(null);
          setVerificationReason('');
          setFormError(null);
        }}
        title="Doctor Credential Verification"
      >
        {verificationDoctor && (
          <div className="space-y-4">
            <p className="text-sm text-text-secondary">
              Update credential verification status for <strong>{verificationDoctor.fullName}</strong>.
            </p>

            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                New Verification Status
              </label>
              <select
                value={newVerificationStatus}
                onChange={(e) => setNewVerificationStatus(e.target.value as DoctorVerificationStatus)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              >
                <option value="VERIFIED">VERIFIED (Approved for OPD practice)</option>
                <option value="PENDING">PENDING (Awaiting credentials review)</option>
                <option value="REJECTED">REJECTED (Declined or invalid license)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Review Notes / Justification
              </label>
              <input
                type="text"
                value={verificationReason}
                onChange={(e) => setVerificationReason(e.target.value)}
                placeholder="Medical license verified, board certified, etc."
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>

            {formError && (
              <div className="p-3 rounded-xl bg-state-error/10 border border-state-error/20 text-state-error text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{formError}</span>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="secondary"
                onClick={() => setVerificationDoctor(null)}
                disabled={submitting}
              >
                Cancel
              </Button>
              <Button
                variant={newVerificationStatus === 'REJECTED' ? 'danger' : 'primary'}
                onClick={handleVerificationSubmit}
                isLoading={submitting}
              >
                Submit Decision
              </Button>
            </div>
          </div>
        )}
      </Dialog>
    </div>
  );
};
