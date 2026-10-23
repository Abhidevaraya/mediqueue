import React, { useState, useEffect, useCallback } from 'react';
import { adminService } from '@/lib/api/adminService';
import { DepartmentResponse } from '@/types/doctor';
import { DepartmentCreateRequest, DepartmentUpdateRequest } from '@/types/admin';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { Dialog } from '@/components/ui/Dialog';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import {
  Plus,
  Edit2,
  RefreshCw,
  Power,
  AlertCircle,
  Building2,
} from 'lucide-react';

export const AdminDepartmentsPage: React.FC = () => {
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modals
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingDept, setEditingDept] = useState<DepartmentResponse | null>(null);
  const [statusTarget, setStatusTarget] = useState<DepartmentResponse | null>(null);

  // Forms
  const [createForm, setCreateForm] = useState<DepartmentCreateRequest>({
    name: '',
    description: '',
  });

  const [editForm, setEditForm] = useState<DepartmentUpdateRequest>({
    name: '',
    description: '',
    active: true,
  });

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const fetchDepartments = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await adminService.getDepartments();
      setDepartments(Array.isArray(data) ? data : []);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load hospital departments';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDepartments();
  }, [fetchDepartments]);

  const handleCreateDepartment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.name.trim()) {
      setFormError('Department name is required');
      return;
    }
    setSubmitting(true);
    setFormError(null);
    try {
      const created = await adminService.createDepartment(createForm);
      setDepartments((prev) => [...prev, created]);
      setShowCreateModal(false);
      setCreateForm({ name: '', description: '' });
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to create department';
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditDepartment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingDept || !editForm.name.trim()) return;
    setSubmitting(true);
    setFormError(null);
    try {
      const updated = await adminService.updateDepartment(editingDept.id, editForm);
      setDepartments((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
      setEditingDept(null);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to update department';
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const isDepartmentActive = (dept: DepartmentResponse) => Boolean(dept.active ?? dept.isActive);

  const handleStatusToggle = async () => {
    if (!statusTarget) return;
    setSubmitting(true);
    try {
      const currentlyActive = isDepartmentActive(statusTarget);
      const updated = await adminService.updateDepartmentStatus(statusTarget.id, !currentlyActive);
      setDepartments((prev) =>
        prev.map((d) =>
          d.id === updated.id
            ? { ...d, ...updated, active: updated.active, isActive: updated.active }
            : d
        )
      );
      setStatusTarget(null);
    } catch (err: unknown) {
      alert(err instanceof Error ? err.message : 'Failed to change department status');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-text-primary tracking-tight">
            Department Catalog
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Configure clinical units, outpatient departments, operational readiness, and services.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={fetchDepartments}>
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Refresh
          </Button>
          <Button variant="primary" size="sm" onClick={() => setShowCreateModal(true)}>
            <Plus className="w-4 h-4 mr-1.5" />
            Add Department
          </Button>
        </div>
      </div>

      {/* Main Table / Grid */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <Spinner size="lg" />
          <p className="text-sm font-medium text-text-secondary">Loading hospital departments...</p>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Departments"
          message={error}
          onRetry={fetchDepartments}
        />
      ) : departments.length === 0 ? (
        <EmptyState
          title="No departments found"
          description="Create your first clinical department to start routing doctor rosters and patient bookings."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {departments.map((dept) => {
            const active = isDepartmentActive(dept);
            return (
              <Card key={dept.id} className="p-5 flex flex-col justify-between">
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <div className="w-9 h-9 rounded-xl bg-brand-lavender/10 text-brand-lavender flex items-center justify-center shrink-0">
                        <Building2 className="w-4 h-4" />
                      </div>
                      <div>
                        <h3 className="font-semibold text-text-primary text-base">{dept.name}</h3>
                        <span className="font-mono text-xs text-text-muted">ID #{dept.id}</span>
                      </div>
                    </div>
                    <Badge variant={active ? 'success' : 'neutral'}>
                      {active ? 'Active' : 'Inactive'}
                    </Badge>
                  </div>

                  <p className="text-xs text-text-secondary mt-3 line-clamp-3">
                    {dept.description || 'No detailed clinical description provided.'}
                  </p>
                </div>

                <div className="mt-4 pt-4 border-t border-surface-border flex items-center justify-between">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setEditingDept(dept);
                      setEditForm({
                        name: dept.name,
                        description: dept.description || '',
                        active,
                      });
                    }}
                  >
                    <Edit2 className="w-3.5 h-3.5 mr-1 text-text-muted" />
                    Edit
                  </Button>

                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setStatusTarget(dept)}
                  >
                    <Power className={`w-3.5 h-3.5 mr-1 ${active ? 'text-state-warning' : 'text-state-success'}`} />
                    {active ? 'Deactivate' : 'Activate'}
                  </Button>
                </div>
              </Card>
            );
          })}
        </div>
      )}

      {/* Create Department Modal */}
      <Dialog
        isOpen={showCreateModal}
        onClose={() => {
          setShowCreateModal(false);
          setFormError(null);
        }}
        title="Add Hospital Department"
      >
        <form onSubmit={handleCreateDepartment} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Department Name *
            </label>
            <input
              type="text"
              required
              value={createForm.name}
              onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
              placeholder="e.g. Cardiology, Neurology, Pediatrics"
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Clinical Description
            </label>
            <textarea
              rows={3}
              value={createForm.description}
              onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
              placeholder="Scope of outpatient services, diagnostic facilities, and specialties..."
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
              type="button"
              onClick={() => setShowCreateModal(false)}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={submitting}>
              Create Department
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Edit Department Modal */}
      <Dialog
        isOpen={Boolean(editingDept)}
        onClose={() => {
          setEditingDept(null);
          setFormError(null);
        }}
        title={`Edit Department: ${editingDept?.name}`}
      >
        <form onSubmit={handleEditDepartment} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Department Name *
            </label>
            <input
              type="text"
              required
              value={editForm.name}
              onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
              Clinical Description
            </label>
            <textarea
              rows={3}
              value={editForm.description || ''}
              onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="activeCheckbox"
              checked={editForm.active ?? true}
              onChange={(e) => setEditForm({ ...editForm, active: e.target.checked })}
              className="w-4 h-4 text-brand-lavender rounded border-surface-border focus:ring-brand-lavender"
            />
            <label htmlFor="activeCheckbox" className="text-sm text-text-primary font-medium">
              Department is active and accepting bookings
            </label>
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
              type="button"
              onClick={() => setEditingDept(null)}
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

      {/* Confirmation Dialog for Status Change */}
      <ConfirmDialog
        isOpen={Boolean(statusTarget)}
        title={statusTarget && isDepartmentActive(statusTarget) ? 'Deactivate Department?' : 'Activate Department?'}
        message={
          statusTarget && isDepartmentActive(statusTarget)
            ? `Are you sure you want to deactivate ${statusTarget.name}? Inactive departments cannot receive new OPD patient bookings.`
            : `Are you sure you want to activate ${statusTarget?.name}? It will become available immediately for doctor assignment and patient appointments.`
        }
        confirmText={statusTarget && isDepartmentActive(statusTarget) ? 'Deactivate' : 'Activate'}
        variant={statusTarget && isDepartmentActive(statusTarget) ? 'danger' : 'primary'}
        isLoading={submitting}
        onConfirm={handleStatusToggle}
        onClose={() => setStatusTarget(null)}
      />
    </div>
  );
};
