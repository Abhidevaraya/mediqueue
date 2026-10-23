import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { ShieldAlert, ArrowLeft, Home, LogOut } from 'lucide-react';
import { useAuth } from '@/lib/auth/AuthContext';

export const UnauthorizedPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const getDashboardPath = () => {
    if (!user) return '/login';
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
    <div className="min-h-screen flex items-center justify-center p-4 bg-brand-bg text-text-primary">
      <Card className="max-w-md w-full shadow-lg border-surface-border text-center">
        <CardContent className="p-8 space-y-6">
          <div className="w-16 h-16 mx-auto rounded-2xl bg-status-error-surface border border-status-error/20 flex items-center justify-center text-status-error">
            <ShieldAlert className="w-8 h-8" />
          </div>

          <div className="space-y-2">
            <h1 className="text-2xl font-bold text-text-primary">Access Restricted</h1>
            <p className="text-sm text-text-secondary leading-relaxed">
              You do not have permission to view this resource. Your account is currently authorized
              with the <span className="font-semibold text-brand-lavender">{user?.role ?? 'UNAUTHENTICATED'}</span> role.
            </p>
          </div>

          <div className="pt-2 flex flex-col gap-2.5">
            {user ? (
              <Link to={getDashboardPath()}>
                <Button variant="primary" size="md" className="w-full" leftIcon={<ArrowLeft className="w-4 h-4" />}>
                  Return to Allowed Dashboard
                </Button>
              </Link>
            ) : (
              <Link to="/login">
                <Button variant="primary" size="md" className="w-full">
                  Sign In with Proper Credentials
                </Button>
              </Link>
            )}

            <div className="flex items-center gap-2">
              <Link to="/" className="flex-1">
                <Button variant="secondary" size="md" className="w-full" leftIcon={<Home className="w-4 h-4" />}>
                  Home
                </Button>
              </Link>
              {user && (
                <Button
                  variant="outline"
                  size="md"
                  onClick={handleLogout}
                  className="flex-1"
                  leftIcon={<LogOut className="w-4 h-4" />}
                >
                  Sign Out
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
