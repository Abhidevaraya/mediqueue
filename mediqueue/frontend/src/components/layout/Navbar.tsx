import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/lib/auth/AuthContext';
import { ThemeToggle } from '@/components/layout/ThemeToggle';
import { ProfileDropdown } from '@/components/layout/ProfileDropdown';
import { Button } from '@/components/ui/Button';
import { Activity, Menu } from 'lucide-react';

export interface NavbarProps {
  onMenuClick?: () => void;
  showMenuButton?: boolean;
}

export const Navbar: React.FC<NavbarProps> = ({ onMenuClick, showMenuButton = false }) => {
  const { user, isAuthenticated } = useAuth();

  const getDashboardPath = () => {
    if (!user) return '/';
    switch (user.role) {
      case 'ADMIN':
        return '/admin';
      case 'DOCTOR':
        return '/doctor';
      case 'PATIENT':
      default:
        return '/patient';
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full bg-surface-elevated/80 backdrop-blur-md border-b border-surface-border/60 transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        {/* Brand logo & mobile menu toggle */}
        <div className="flex items-center gap-3">
          {showMenuButton && (
            <button
              type="button"
              onClick={onMenuClick}
              className="lg:hidden p-2 rounded-xl text-text-secondary hover:text-text-primary hover:bg-surface-subtle transition-colors focus-visible:outline-none focus-visible-ring"
              aria-label="Open navigation menu"
            >
              <Menu className="w-5 h-5" />
            </button>
          )}

          <Link to="/" className="flex items-center gap-2.5 group select-none">
            <div className="w-9 h-9 rounded-xl bg-brand-lavender flex items-center justify-center text-white shadow-soft-sm group-hover:bg-brand-lavender-hover transition-colors">
              <Activity className="w-5 h-5" />
            </div>
            <div className="flex flex-col">
              <span className="text-lg font-bold font-display tracking-tight text-brand-indigo dark:text-white leading-none">
                MediQueue
              </span>
              <span className="text-[10px] text-brand-lavender font-semibold tracking-wider uppercase mt-0.5">
                Hospital Platform
              </span>
            </div>
          </Link>
        </div>

        {/* Right action group */}
        <div className="flex items-center gap-3">
          <ThemeToggle />

          {isAuthenticated ? (
            <div className="flex items-center gap-3">
              <Link to={getDashboardPath()} className="hidden md:block">
                <Button variant="secondary" size="sm">
                  Dashboard
                </Button>
              </Link>
              <ProfileDropdown />
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link to="/login">
                <Button variant="ghost" size="sm">
                  Sign in
                </Button>
              </Link>
              <Link to="/register">
                <Button variant="primary" size="sm">
                  Register
                </Button>
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
