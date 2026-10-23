import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '@/lib/auth/AuthContext';
import { cn } from '@/lib/utils/cn';
import {
  Calendar,
  Users,
  Activity,
  FileText,
  BarChart3,
  ShieldAlert,
  ClipboardList,
  Clock,
  Layers,
  Building2,
  Stethoscope,
} from 'lucide-react';

export interface SidebarNavProps {
  onItemClick?: () => void;
}

export const Sidebar: React.FC<SidebarNavProps> = ({ onItemClick }) => {
  const { user } = useAuth();

  if (!user) return null;

  const getNavItems = () => {
    switch (user.role) {
      case 'ADMIN':
        return [
          { label: 'Overview', to: '/admin', icon: Layers },
          { label: 'User Directory', to: '/admin/users', icon: Users },
          { label: 'Doctor Management', to: '/admin/doctors', icon: Stethoscope },
          { label: 'Department Catalog', to: '/admin/departments', icon: Building2 },
          { label: 'Queue Operations', to: '/admin/queues', icon: Clock },
          { label: 'Doctor Schedules', to: '/admin/schedules', icon: Calendar },
          { label: 'Analytics', to: '/admin/analytics', icon: BarChart3 },
          { label: 'Audit Trail', to: '/admin/audit', icon: ShieldAlert },
        ];
      case 'DOCTOR':
        return [
          { label: 'Clinical Dashboard', to: '/doctor', icon: Activity },
          { label: 'OPD Queue Console', to: '/doctor/queue', icon: Clock },
          { label: 'Consultations & Rx', to: '/doctor/consultations', icon: ClipboardList },
          { label: 'My Schedule', to: '/doctor/schedule', icon: Calendar },
        ];
      case 'PATIENT':
      default:
        return [
          { label: 'My Health Home', to: '/patient', icon: Layers },
          { label: 'Find a Specialist', to: '/patient/doctors', icon: Users },
          { label: 'My Appointments', to: '/patient/appointments', icon: Calendar },
          { label: 'Live Queue Status', to: '/patient/queue', icon: Clock },
          { label: 'Medical Records & Rx', to: '/patient/records', icon: FileText },
        ];
    }
  };

  const navItems = getNavItems();

  return (
    <aside className="w-64 shrink-0 bg-surface-elevated border-r border-surface-border flex flex-col h-[calc(100vh-4rem)] sticky top-16 transition-colors">
      <div className="p-4 flex flex-col gap-1 flex-1 overflow-y-auto">
        <div className="px-3 py-2 text-[11px] font-bold uppercase tracking-wider text-text-muted">
          {user.role} Navigation
        </div>

        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/patient' || item.to === '/doctor' || item.to === '/admin'}
              onClick={onItemClick}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 select-none',
                  isActive
                    ? 'bg-brand-lavender text-white shadow-soft-sm font-semibold'
                    : 'text-text-secondary hover:text-text-primary hover:bg-surface-subtle'
                )
              }
            >
              <Icon className="w-4 h-4 shrink-0" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </div>

      <div className="p-4 border-t border-surface-border/60">
        <div className="px-3 py-2 rounded-xl bg-surface-subtle/70 text-xs text-text-muted">
          <p className="font-semibold text-text-primary">MediQueue Platform</p>
          <p className="mt-0.5">Clinical appointment & live OPD queue management system.</p>
        </div>
      </div>
    </aside>
  );
};
