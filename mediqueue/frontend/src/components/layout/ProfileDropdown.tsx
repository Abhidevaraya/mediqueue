import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '@/lib/auth/AuthContext';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { LogOut, ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

export const ProfileDropdown: React.FC = () => {
  const { user, logout } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  if (!user) return null;

  const getRoleBadgeVariant = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'primary';
      case 'DOCTOR':
        return 'success';
      default:
        return 'neutral';
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className="flex items-center gap-2.5 p-1.5 rounded-xl hover:bg-surface-subtle transition-colors focus-visible:outline-none focus-visible-ring"
        aria-expanded={isOpen}
        aria-haspopup="true"
        aria-label="User account menu"
      >
        <Avatar name={user.fullName} size="sm" />
        <div className="hidden sm:flex flex-col text-left">
          <span className="text-xs font-semibold text-text-primary leading-tight truncate max-w-[120px]">
            {user.fullName}
          </span>
          <span className="text-[10px] text-text-muted capitalize">
            {user.role.toLowerCase()}
          </span>
        </div>
        <ChevronDown
          className={cn('w-4 h-4 text-text-muted transition-transform duration-200', isOpen && 'rotate-180')}
        />
      </button>

      {isOpen && (
        <div
          className="absolute right-0 mt-2 w-56 rounded-2xl bg-surface-elevated border border-surface-border shadow-soft-lg py-2 z-50 animate-scale-up"
          role="menu"
        >
          <div className="px-4 py-2.5 border-b border-surface-border/60">
            <p className="text-sm font-bold text-text-primary truncate">{user.fullName}</p>
            <p className="text-xs text-text-secondary truncate mt-0.5">{user.email}</p>
            <div className="mt-2">
              <Badge variant={getRoleBadgeVariant(user.role)} size="sm">
                {user.role}
              </Badge>
            </div>
          </div>

          <div className="p-1">
            <button
              type="button"
              onClick={async () => {
                setIsOpen(false);
                await logout();
              }}
              className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-status-error hover:bg-status-error-surface/60 rounded-xl transition-colors font-medium text-left"
              role="menuitem"
            >
              <LogOut className="w-4 h-4 shrink-0" />
              <span>Log out</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
