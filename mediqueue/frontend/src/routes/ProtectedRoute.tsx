import React from 'react';
import { Navigate, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '@/lib/auth/AuthContext';
import { Spinner } from '@/components/ui/Spinner';

export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-brand-bg gap-3 text-text-secondary">
        <Spinner size="lg" className="text-brand-lavender" />
        <p className="text-sm font-medium animate-pulse">Restoring secure session...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    // Save location to redirect back after successful login
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
};
