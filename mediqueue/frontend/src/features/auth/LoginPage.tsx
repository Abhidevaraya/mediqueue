import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { AuthLayout } from '@/features/auth/AuthLayout';
import { FormField } from '@/components/ui/FormField';
import { Input } from '@/components/ui/Input';
import { PasswordInput } from '@/components/ui/PasswordInput';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/lib/feedback/ToastContext';
import { AppApiError } from '@/lib/api/client';
import { Mail, Lock, AlertCircle } from 'lucide-react';

const loginSchema = z.object({
  email: z.string().min(1, 'Email address is required').email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = async (values: LoginFormValues) => {
    setServerError(null);
    try {
      const response = await login(values);
      toast.success(`Welcome back, ${response.user.fullName}!`, 'Authenticated');

      // Determine safe redirect
      const fromPath = (location.state as { from?: { pathname?: string } })?.from?.pathname;
      const isSafeInternalPath = fromPath && fromPath.startsWith('/') && !fromPath.startsWith('//');

      if (isSafeInternalPath) {
        navigate(fromPath, { replace: true });
        return;
      }

      // Default role dashboard redirect
      switch (response.user.role) {
        case 'ADMIN':
          navigate('/admin', { replace: true });
          break;
        case 'DOCTOR':
          navigate('/doctor', { replace: true });
          break;
        case 'PATIENT':
        default:
          navigate('/patient', { replace: true });
          break;
      }
    } catch (err) {
      if (err instanceof AppApiError) {
        setServerError(err.message);
        toast.error(err.message, 'Sign In Failed');
      } else {
        setServerError('An unexpected error occurred. Please try again.');
        toast.error('Could not connect to authentication service');
      }
    }
  };

  return (
    <AuthLayout
      title="Welcome Back"
      subtitle="Enter your credentials to access your MediQueue account"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {serverError && (
          <div
            role="alert"
            className="p-3.5 rounded-xl bg-status-error-surface border border-status-error/30 text-status-error text-sm flex items-start gap-2.5"
          >
            <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
            <p className="leading-snug">{serverError}</p>
          </div>
        )}

        <FormField id="email" label="Email Address" required error={errors.email?.message}>
          <Input
            type="email"
            placeholder="patient@example.com"
            autoComplete="email"
            leftIcon={<Mail className="w-4 h-4" />}
            {...register('email')}
          />
        </FormField>

        <FormField id="password" label="Password" required error={errors.password?.message}>
          <PasswordInput
            placeholder="••••••••"
            autoComplete="current-password"
            leftIcon={<Lock className="w-4 h-4" />}
            {...register('password')}
          />
        </FormField>

        <div className="pt-2">
          <Button
            type="submit"
            variant="primary"
            size="md"
            className="w-full"
            isLoading={isSubmitting}
          >
            Sign In
          </Button>
        </div>

        <div className="pt-4 border-t border-surface-border text-center">
          <p className="text-sm text-text-secondary">
            Don't have a patient account?{' '}
            <Link
              to="/register"
              className="text-brand-lavender font-semibold hover:underline focus-visible:outline-none focus-visible-ring rounded"
            >
              Register here
            </Link>
          </p>
        </div>
      </form>
    </AuthLayout>
  );
};
