import React, { useState, useEffect, useCallback } from 'react';
import { adminService } from '@/lib/api/adminService';
import { UserResponse, AccountStatus } from '@/types/admin';
import { UserRole } from '@/types/auth';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { Dialog } from '@/components/ui/Dialog';
import {
  Search,
  Shield,
  UserX,
  UserCheck,
  Eye,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';

export const AdminUserDirectoryPage: React.FC = () => {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Pagination
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [selectedRole, setSelectedRole] = useState<UserRole | ''>('');
  const [selectedStatus, setSelectedStatus] = useState<AccountStatus | ''>('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modals
  const [viewUser, setViewUser] = useState<UserResponse | null>(null);
  const [statusTarget, setStatusTarget] = useState<{ user: UserResponse; newStatus: AccountStatus } | null>(null);
  const [statusReason, setStatusReason] = useState('');
  const [roleTarget, setRoleTarget] = useState<UserResponse | null>(null);
  const [newRole, setNewRole] = useState<UserRole>('PATIENT');
  const [roleReason, setRoleReason] = useState('');

  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 400);
    return () => clearTimeout(handler);
  }, [search]);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await adminService.getUsers({
        search: debouncedSearch.trim() || undefined,
        role: (selectedRole as UserRole) || undefined,
        status: (selectedStatus as AccountStatus) || undefined,
        page,
        size: 15,
        sortBy: 'createdAt',
        sortDir: 'desc',
      });
      setUsers(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to retrieve user directory';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, selectedRole, selectedStatus, page]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleStatusChange = async () => {
    if (!statusTarget) return;
    setActionLoading(true);
    setActionError(null);
    try {
      const updated = await adminService.updateUserStatus(
        statusTarget.user.id,
        statusTarget.newStatus
      );
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      setStatusTarget(null);
      setStatusReason('');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to update account status';
      setActionError(message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRoleChange = async () => {
    if (!roleTarget) return;
    setActionLoading(true);
    setActionError(null);
    try {
      const updated = await adminService.updateUserRole(
        roleTarget.id,
        newRole
      );
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
      setRoleTarget(null);
      setRoleReason('');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to reassign user role';
      setActionError(message);
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusBadgeVariant = (status: AccountStatus) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'BLOCKED':
        return 'error';
      case 'PENDING':
        return 'warning';
      default:
        return 'neutral';
    }
  };

  const getRoleBadgeVariant = (role: UserRole): 'primary' | 'warning' | 'neutral' => {
    switch (role) {
      case 'ADMIN':
        return 'primary';
      case 'DOCTOR':
        return 'warning';
      case 'PATIENT':
      default:
        return 'neutral';
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-text-primary tracking-tight">
            User Directory & Accounts
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Search hospital accounts, manage authentication access, and modify role authorizations.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="secondary" size="sm" onClick={fetchUsers}>
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Refresh
          </Button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <Card className="p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
            <input
              type="text"
              placeholder="Search by name, email, phone..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            />
          </div>

          <div>
            <select
              value={selectedRole}
              onChange={(e) => {
                setSelectedRole(e.target.value as UserRole | '');
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            >
              <option value="">All Roles</option>
              <option value="PATIENT">Patient</option>
              <option value="DOCTOR">Doctor</option>
              <option value="ADMIN">Administrator</option>
            </select>
          </div>

          <div>
            <select
              value={selectedStatus}
              onChange={(e) => {
                setSelectedStatus(e.target.value as AccountStatus | '');
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
            >
              <option value="">All Account Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="BLOCKED">Blocked</option>
              <option value="PENDING">Pending</option>
            </select>
          </div>

          <div className="flex items-center justify-between sm:justify-end text-xs text-text-secondary px-2">
            <span>{totalElements} total accounts</span>
          </div>
        </div>
      </Card>

      {/* Main Table */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3">
          <Spinner size="lg" />
          <p className="text-sm font-medium text-text-secondary">Querying user accounts...</p>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Users"
          message={error}
          onRetry={fetchUsers}
        />
      ) : users.length === 0 ? (
        <EmptyState
          title="No users found"
          description="Try adjusting your search criteria or role filters to view accounts."
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="border-b border-surface-border bg-surface-subtle/50 text-xs font-semibold text-text-muted uppercase tracking-wider">
                  <th className="px-5 py-3">User ID</th>
                  <th className="px-5 py-3">Full Name</th>
                  <th className="px-5 py-3">Email & Contact</th>
                  <th className="px-5 py-3">Role</th>
                  <th className="px-5 py-3">Account Status</th>
                  <th className="px-5 py-3">Registered Date</th>
                  <th className="px-5 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-border">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-surface-subtle/40 transition-colors">
                    <td className="px-5 py-3.5 font-mono text-xs text-text-muted">
                      #{u.id}
                    </td>
                    <td className="px-5 py-3.5 font-medium text-text-primary">
                      {u.fullName}
                    </td>
                    <td className="px-5 py-3.5 text-text-secondary">
                      <div>{u.email}</div>
                      {u.phoneNumber && (
                        <div className="text-xs text-text-muted">{u.phoneNumber}</div>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge variant={getRoleBadgeVariant(u.role)}>
                        {u.role}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge variant={getStatusBadgeVariant(u.accountStatus)}>
                        {u.accountStatus}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5 text-xs text-text-muted">
                      {new Date(u.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setViewUser(u)}
                          title="View Account Details"
                        >
                          <Eye className="w-4 h-4 text-text-muted" />
                        </Button>

                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            setRoleTarget(u);
                            setNewRole(u.role);
                          }}
                          title="Change User Role"
                        >
                          <Shield className="w-4 h-4 text-brand-lavender" />
                        </Button>

                        {u.accountStatus === 'ACTIVE' ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() =>
                              setStatusTarget({ user: u, newStatus: 'BLOCKED' })
                            }
                            title="Block User Account"
                          >
                            <UserX className="w-4 h-4 text-state-error" />
                          </Button>
                        ) : (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() =>
                              setStatusTarget({ user: u, newStatus: 'ACTIVE' })
                            }
                            title="Activate User Account"
                          >
                            <UserCheck className="w-4 h-4 text-state-success" />
                          </Button>
                        )}
                      </div>
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
                Showing page {page + 1} of {totalPages} ({totalElements} users)
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

      {/* User Details Dialog */}
      <Dialog
        isOpen={Boolean(viewUser)}
        onClose={() => setViewUser(null)}
        title="User Account Profile"
      >
        {viewUser && (
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-surface-subtle border border-surface-border space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-text-muted uppercase">System ID</span>
                <span className="font-mono text-sm text-text-primary">#{viewUser.id}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-text-muted uppercase">Full Name</span>
                <span className="font-medium text-sm text-text-primary">{viewUser.fullName}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-text-muted uppercase">Email Address</span>
                <span className="text-sm text-text-primary">{viewUser.email}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-text-muted uppercase">Phone</span>
                <span className="text-sm text-text-primary">{viewUser.phoneNumber || 'None'}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-text-muted uppercase">Authorization Role</span>
                <Badge variant={getRoleBadgeVariant(viewUser.role)}>{viewUser.role}</Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-text-muted uppercase">Account Status</span>
                <Badge variant={getStatusBadgeVariant(viewUser.accountStatus)}>{viewUser.accountStatus}</Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-text-muted uppercase">Created At</span>
                <span className="text-xs text-text-muted">{new Date(viewUser.createdAt).toLocaleString()}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-text-muted uppercase">Last Updated</span>
                <span className="text-xs text-text-muted">{new Date(viewUser.updatedAt).toLocaleString()}</span>
              </div>
            </div>
            <div className="flex justify-end">
              <Button variant="secondary" onClick={() => setViewUser(null)}>
                Close
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* Account Status Confirmation Dialog */}
      <Dialog
        isOpen={Boolean(statusTarget)}
        onClose={() => {
          setStatusTarget(null);
          setStatusReason('');
          setActionError(null);
        }}
        title={`Confirm Account Status Change: ${statusTarget?.newStatus}`}
      >
        {statusTarget && (
          <div className="space-y-4">
            <p className="text-sm text-text-secondary">
              You are about to change the status of <strong>{statusTarget.user.fullName}</strong> ({statusTarget.user.email}) to{' '}
              <strong className={statusTarget.newStatus === 'BLOCKED' ? 'text-state-error' : 'text-state-success'}>
                {statusTarget.newStatus}
              </strong>.
            </p>

            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Reason / Note (optional)
              </label>
              <textarea
                rows={2}
                value={statusReason}
                onChange={(e) => setStatusReason(e.target.value)}
                placeholder="Reason for blocking or activating this account..."
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>

            {actionError && (
              <div className="p-3 rounded-xl bg-state-error/10 border border-state-error/20 text-state-error text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="secondary"
                onClick={() => {
                  setStatusTarget(null);
                  setStatusReason('');
                }}
                disabled={actionLoading}
              >
                Cancel
              </Button>
              <Button
                variant={statusTarget.newStatus === 'BLOCKED' ? 'danger' : 'primary'}
                onClick={handleStatusChange}
                isLoading={actionLoading}
              >
                Confirm {statusTarget.newStatus}
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      {/* Role Reassignment Dialog */}
      <Dialog
        isOpen={Boolean(roleTarget)}
        onClose={() => {
          setRoleTarget(null);
          setRoleReason('');
          setActionError(null);
        }}
        title="Reassign Authorization Role"
      >
        {roleTarget && (
          <div className="space-y-4">
            <p className="text-sm text-text-secondary">
              Reassign security role for <strong>{roleTarget.fullName}</strong> ({roleTarget.email}). Current role is{' '}
              <Badge variant={getRoleBadgeVariant(roleTarget.role)}>{roleTarget.role}</Badge>.
            </p>

            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Select New Role
              </label>
              <select
                value={newRole}
                onChange={(e) => setNewRole(e.target.value as UserRole)}
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              >
                <option value="PATIENT">PATIENT</option>
                <option value="DOCTOR">DOCTOR</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-text-muted uppercase mb-1">
                Administrative Justification
              </label>
              <input
                type="text"
                value={roleReason}
                onChange={(e) => setRoleReason(e.target.value)}
                placeholder="Reason for role change (e.g. staff promotion, credential verification)"
                className="w-full px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-elevated text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40"
              />
            </div>

            {actionError && (
              <div className="p-3 rounded-xl bg-state-error/10 border border-state-error/20 text-state-error text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{actionError}</span>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <Button
                variant="secondary"
                onClick={() => {
                  setRoleTarget(null);
                  setRoleReason('');
                }}
                disabled={actionLoading}
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleRoleChange}
                isLoading={actionLoading}
              >
                Apply Role Update
              </Button>
            </div>
          </div>
        )}
      </Dialog>
    </div>
  );
};
