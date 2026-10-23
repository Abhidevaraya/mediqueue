import React from 'react';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/lib/feedback/ToastContext';
import { UserRole } from '@/types/auth';
import {
  ShieldCheck,
  UserCheck,
  RefreshCw,
  Calendar,
  Layers,
  Sparkles,
  Info,
} from 'lucide-react';

interface DashboardPlaceholderProps {
  expectedRole: UserRole;
  title: string;
  description: string;
}

export const DashboardPlaceholder: React.FC<DashboardPlaceholderProps> = ({
  expectedRole,
  title,
  description,
}) => {
  const { user, refreshProfile } = useAuth();
  const toast = useToast();
  const [isRefreshing, setIsRefreshing] = React.useState(false);

  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await refreshProfile();
      toast.info('Profile and authorization data refreshed from backend', 'Profile Synced');
    } catch {
      toast.error('Failed to sync profile with server');
    } finally {
      setIsRefreshing(false);
    }
  };

  const roleBadgeVariant = () => {
    switch (user?.role) {
      case 'ADMIN':
        return 'warning';
      case 'DOCTOR':
        return 'primary';
      case 'PATIENT':
      default:
        return 'success';
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold text-text-primary tracking-tight font-heading">
              {title}
            </h1>
            <Badge variant={roleBadgeVariant() as any} size="md">
              {user?.role}
            </Badge>
          </div>
          <p className="text-sm text-text-secondary mt-1">{description}</p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleRefresh}
            isLoading={isRefreshing}
            leftIcon={<RefreshCw className="w-4 h-4" />}
          >
            Sync Profile
          </Button>
        </div>
      </div>

      {/* Session Verification Card */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="md:col-span-2 border-surface-border">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2 text-base">
                <UserCheck className="w-5 h-5 text-brand-lavender" />
                Active Authenticated Session
              </CardTitle>
              <Badge variant="success" size="sm">
                {user?.accountStatus ?? 'ACTIVE'}
              </Badge>
            </div>
            <CardDescription>
              Backend session validated via Spring Boot JWT authentication
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div className="p-3.5 rounded-xl bg-surface-lavender/30 border border-surface-border space-y-1">
                <span className="text-xs text-text-secondary font-medium">User ID</span>
                <p className="font-semibold text-text-primary">#{user?.id}</p>
              </div>

              <div className="p-3.5 rounded-xl bg-surface-lavender/30 border border-surface-border space-y-1">
                <span className="text-xs text-text-secondary font-medium">Full Name</span>
                <p className="font-semibold text-text-primary">{user?.fullName}</p>
              </div>

              <div className="p-3.5 rounded-xl bg-surface-lavender/30 border border-surface-border space-y-1">
                <span className="text-xs text-text-secondary font-medium">Registered Email</span>
                <p className="font-semibold text-text-primary truncate">{user?.email}</p>
              </div>

              <div className="p-3.5 rounded-xl bg-surface-lavender/30 border border-surface-border space-y-1">
                <span className="text-xs text-text-secondary font-medium">Phone Number</span>
                <p className="font-semibold text-text-primary">{user?.phoneNumber || 'Not provided'}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Phase Scope Status */}
        <Card className="border-surface-border flex flex-col justify-between">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <ShieldCheck className="w-5 h-5 text-brand-lavender" />
              Phase 1 Status
            </CardTitle>
            <CardDescription>Foundation & Routing Verification</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2 text-xs text-text-secondary leading-relaxed">
              <div className="flex items-center gap-2 text-status-success font-medium">
                <Sparkles className="w-4 h-4 shrink-0" />
                <span>Authentication & Session Active</span>
              </div>
              <div className="flex items-center gap-2 text-status-success font-medium">
                <Sparkles className="w-4 h-4 shrink-0" />
                <span>Protected {expectedRole} Route Guard</span>
              </div>
              <div className="flex items-center gap-2 text-status-success font-medium">
                <Sparkles className="w-4 h-4 shrink-0" />
                <span>Lavender Design System & Shell Loaded</span>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-surface-lavender/40 border border-brand-lavender/20 text-xs text-text-secondary space-y-1">
              <div className="flex items-center gap-1.5 font-semibold text-brand-deep dark:text-brand-lavender">
                <Info className="w-4 h-4 shrink-0" />
                <span>Phase 2 Readiness</span>
              </div>
              <p>
                Appointment booking, live queue tickets, and doctor consultations will integrate into
                this layout shell during Frontend Phase 2.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Feature Module Cards for Phase 2 Preview */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-surface-border">
          <CardContent className="p-6 space-y-3">
            <div className="w-10 h-10 rounded-xl bg-brand-surface flex items-center justify-center text-brand-lavender">
              <Calendar className="w-5 h-5" />
            </div>
            <h2 className="font-bold text-text-primary text-base">Appointment Orchestration</h2>
            <p className="text-xs text-text-secondary leading-relaxed">
              Schedule, reschedule, or cancel consultations across hospital departments with instant availability checks.
            </p>
            <Badge variant="neutral" size="sm">Phase 2 Module</Badge>
          </CardContent>
        </Card>

        <Card className="border-surface-border">
          <CardContent className="p-6 space-y-3">
            <div className="w-10 h-10 rounded-xl bg-brand-surface flex items-center justify-center text-brand-lavender">
              <Layers className="w-5 h-5" />
            </div>
            <h2 className="font-bold text-text-primary text-base">Live STOMP Queue</h2>
            <p className="text-xs text-text-secondary leading-relaxed">
              Live token position, predicted wait calculation, and instant doctor-call notifications.
            </p>
            <Badge variant="neutral" size="sm">Phase 3 Module</Badge>
          </CardContent>
        </Card>

        <Card className="border-surface-border">
          <CardContent className="p-6 space-y-3">
            <div className="w-10 h-10 rounded-xl bg-brand-surface flex items-center justify-center text-brand-lavender">
              <UserCheck className="w-5 h-5" />
            </div>
            <h2 className="font-bold text-text-primary text-base">Clinical Records & Rx</h2>
            <p className="text-xs text-text-secondary leading-relaxed">
              Patient consultations, digital prescriptions, and follow-up clinical documentation.
            </p>
            <Badge variant="neutral" size="sm">Phase 4 Module</Badge>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
