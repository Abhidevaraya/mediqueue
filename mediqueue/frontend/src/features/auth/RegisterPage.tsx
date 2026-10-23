import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { AuthLayout } from '@/features/auth/AuthLayout';
import { FormField } from '@/components/ui/FormField';
import { Input } from '@/components/ui/Input';
import { PasswordInput } from '@/components/ui/PasswordInput';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/lib/feedback/ToastContext';
import { AppApiError } from '@/lib/api/client';
import { doctorService } from '@/lib/api/doctorService';
import { DepartmentResponse } from '@/types/doctor';
import {
  User,
  Mail,
  Lock,
  Phone,
  AlertCircle,
  CheckCircle2,
  Stethoscope,
  Building2,
  GraduationCap,
  Briefcase,
  DollarSign,
  FileText,
  Clock,
} from 'lucide-react';

const patientSchema = z
  .object({
    fullName: z
      .string()
      .trim()
      .min(2, 'Full name must be at least 2 characters')
      .max(100, 'Full name cannot exceed 100 characters'),
    email: z
      .string()
      .trim()
      .min(1, 'Email address is required')
      .email('Please enter a valid email address'),
    phoneNumber: z
      .string()
      .trim()
      .refine(
        (val) => !val || /^[+]?[(]?[0-9]{1,4}[)]?[-\s./0-9]{6,15}$/.test(val),
        'Please enter a valid phone number (10-15 digits)'
      )
      .optional()
      .or(z.literal('')),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
      .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
      .regex(/[0-9]/, 'Password must contain at least one number')
      .regex(/[@$!%*?&#]/, 'Password must contain at least one special character (@$!%*?&#)'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

const doctorSchema = z
  .object({
    fullName: z
      .string()
      .trim()
      .min(2, 'Full name must be at least 2 characters')
      .max(100, 'Full name cannot exceed 100 characters'),
    email: z
      .string()
      .trim()
      .min(1, 'Email address is required')
      .email('Please enter a valid email address'),
    phoneNumber: z
      .string()
      .trim()
      .refine(
        (val) => !val || /^[+]?[(]?[0-9]{1,4}[)]?[-\s./0-9]{6,15}$/.test(val),
        'Please enter a valid phone number (10-15 digits)'
      )
      .optional()
      .or(z.literal('')),
    departmentId: z.coerce.number().min(1, 'Please select a hospital department'),
    specialty: z
      .string()
      .trim()
      .min(2, 'Specialty is required (e.g. Cardiology, Pediatrics)')
      .max(100),
    qualification: z
      .string()
      .trim()
      .min(2, 'Qualification is required (e.g. MBBS, MD)')
      .max(150),
    yearsOfExperience: z.coerce.number().min(0, 'Experience must be 0 or greater'),
    consultationFee: z.coerce.number().min(0, 'Consultation fee must be non-negative'),
    professionalBio: z.string().max(1000).optional().or(z.literal('')),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
      .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
      .regex(/[0-9]/, 'Password must contain at least one number')
      .regex(/[@$!%*?&#]/, 'Password must contain at least one special character (@$!%*?&#)'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type PatientFormValues = z.infer<typeof patientSchema>;
type DoctorFormValues = z.infer<typeof doctorSchema>;

export const RegisterPage: React.FC = () => {
  const [roleTab, setRoleTab] = useState<'PATIENT' | 'DOCTOR'>('PATIENT');
  const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
  const [loadingDepartments, setLoadingDepartments] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [doctorSubmittedName, setDoctorSubmittedName] = useState<string | null>(null);

  const { register: registerPatient, registerDoctor } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  // Load departments for doctor registration
  useEffect(() => {
    let isMounted = true;
    const fetchDepts = async () => {
      setLoadingDepartments(true);
      try {
        const res = await doctorService.getDepartments(0, 100);
        if (isMounted) {
          setDepartments(res.content);
        }
      } catch {
        // Fallback departments if offline
      } finally {
        if (isMounted) {
          setLoadingDepartments(false);
        }
      }
    };
    fetchDepts();
    return () => {
      isMounted = false;
    };
  }, []);

  // Form hooks
  const patientForm = useForm<PatientFormValues>({
    resolver: zodResolver(patientSchema),
    defaultValues: {
      fullName: '',
      email: '',
      phoneNumber: '',
      password: '',
      confirmPassword: '',
    },
  });

  const doctorForm = useForm<DoctorFormValues>({
    resolver: zodResolver(doctorSchema),
    defaultValues: {
      fullName: '',
      email: '',
      phoneNumber: '',
      departmentId: 0,
      specialty: '',
      qualification: '',
      yearsOfExperience: 0,
      consultationFee: 50,
      professionalBio: '',
      password: '',
      confirmPassword: '',
    },
  });

  const currentPassword =
    roleTab === 'PATIENT' ? patientForm.watch('password', '') : doctorForm.watch('password', '');

  const passwordChecks = [
    { label: 'At least 8 characters', met: currentPassword.length >= 8 },
    { label: 'One uppercase letter', met: /[A-Z]/.test(currentPassword) },
    { label: 'One lowercase letter', met: /[a-z]/.test(currentPassword) },
    { label: 'One number', met: /[0-9]/.test(currentPassword) },
    { label: 'One special symbol (@$!%*?&#)', met: /[@$!%*?&#]/.test(currentPassword) },
  ];

  const onPatientSubmit = async (values: PatientFormValues) => {
    setServerError(null);
    try {
      const response = await registerPatient({
        fullName: values.fullName,
        email: values.email,
        password: values.password,
        phoneNumber: values.phoneNumber ? values.phoneNumber : undefined,
      });

      toast.success(
        `Welcome to MediQueue, ${response.user.fullName}! Your patient account is active.`,
        'Registration Successful'
      );
      navigate('/patient', { replace: true });
    } catch (err) {
      if (err instanceof AppApiError) {
        setServerError(err.message);
        toast.error(err.message, 'Registration Failed');
      } else {
        setServerError('An unexpected error occurred during registration. Please try again.');
        toast.error('Could not connect to registration service');
      }
    }
  };

  const onDoctorSubmit = async (values: DoctorFormValues) => {
    setServerError(null);
    try {
      const response = await registerDoctor({
        fullName: values.fullName,
        email: values.email,
        password: values.password,
        phoneNumber: values.phoneNumber ? values.phoneNumber : undefined,
        departmentId: values.departmentId,
        specialty: values.specialty,
        qualification: values.qualification,
        yearsOfExperience: values.yearsOfExperience,
        consultationFee: values.consultationFee,
        professionalBio: values.professionalBio || undefined,
      });

      toast.success(
        `Application submitted for Dr. ${response.user.fullName}! Pending administrator approval.`,
        'Application Submitted'
      );
      setDoctorSubmittedName(response.user.fullName);
    } catch (err) {
      if (err instanceof AppApiError) {
        setServerError(err.message);
        toast.error(err.message, 'Registration Failed');
      } else {
        setServerError('An unexpected error occurred during doctor registration. Please try again.');
        toast.error('Could not connect to registration service');
      }
    }
  };

  if (doctorSubmittedName) {
    return (
      <AuthLayout
        title="Application Submitted"
        subtitle="Your medical practitioner account is pending administrator review"
      >
        <div className="p-6 bg-surface-card rounded-2xl border border-surface-border text-center space-y-4">
          <div className="w-14 h-14 mx-auto rounded-full bg-status-warning-surface border border-status-warning/30 flex items-center justify-center text-status-warning">
            <Clock className="w-7 h-7" />
          </div>
          <div className="space-y-1">
            <h3 className="text-lg font-bold text-text-primary">
              Thank you, Dr. {doctorSubmittedName}!
            </h3>
            <p className="text-sm text-text-secondary leading-relaxed">
              Your registration has been submitted successfully. In accordance with clinical hospital guidelines, doctor profiles require administrator verification before access is granted.
            </p>
          </div>
          <div className="p-4 bg-surface-lavender/40 rounded-xl text-xs text-text-secondary border border-surface-border text-left space-y-2">
            <p className="font-semibold text-text-primary">What happens next?</p>
            <p>1. Hospital administration reviews your medical credentials and department assignment.</p>
            <p>2. Upon approval, your account status is activated.</p>
            <p>3. You can then sign in directly using your registered email and password.</p>
          </div>
          <div className="pt-2">
            <Button
              type="button"
              variant="primary"
              size="md"
              className="w-full"
              onClick={() => navigate('/login')}
            >
              Return to Sign In
            </Button>
          </div>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      title={roleTab === 'PATIENT' ? 'Create Patient Account' : 'Register as a Doctor'}
      subtitle={
        roleTab === 'PATIENT'
          ? 'Register as a patient to schedule appointments and receive digital queue tokens'
          : 'Create your practitioner profile to manage patient queues, appointments, and consultations'
      }
    >
      {/* Role Selection Tabs */}
      <div className="flex p-1 bg-surface-lavender/50 rounded-xl mb-6 border border-surface-border">
        <button
          type="button"
          onClick={() => {
            setRoleTab('PATIENT');
            setServerError(null);
          }}
          className={`flex-1 py-2.5 px-3 text-sm font-semibold rounded-lg transition-all flex items-center justify-center gap-2 ${
            roleTab === 'PATIENT'
              ? 'bg-white text-brand-lavender shadow-sm'
              : 'text-text-secondary hover:text-text-primary'
          }`}
        >
          <User className="w-4 h-4" />
          Patient Registration
        </button>
        <button
          type="button"
          onClick={() => {
            setRoleTab('DOCTOR');
            setServerError(null);
          }}
          className={`flex-1 py-2.5 px-3 text-sm font-semibold rounded-lg transition-all flex items-center justify-center gap-2 ${
            roleTab === 'DOCTOR'
              ? 'bg-white text-brand-lavender shadow-sm'
              : 'text-text-secondary hover:text-text-primary'
          }`}
        >
          <Stethoscope className="w-4 h-4" />
          Doctor Registration
        </button>
      </div>

      {serverError && (
        <div
          role="alert"
          className="p-3.5 mb-4 rounded-xl bg-status-error-surface border border-status-error/30 text-status-error text-sm flex items-start gap-2.5"
        >
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <p className="leading-snug">{serverError}</p>
        </div>
      )}

      {/* PATIENT FORM */}
      {roleTab === 'PATIENT' && (
        <form onSubmit={patientForm.handleSubmit(onPatientSubmit)} className="space-y-4" noValidate>
          <FormField
            id="fullName"
            label="Full Name"
            required
            error={patientForm.formState.errors.fullName?.message}
          >
            <Input
              placeholder="Jane Doe"
              autoComplete="name"
              leftIcon={<User className="w-4 h-4" />}
              {...patientForm.register('fullName')}
            />
          </FormField>

          <FormField
            id="email"
            label="Email Address"
            required
            error={patientForm.formState.errors.email?.message}
          >
            <Input
              type="email"
              placeholder="jane.doe@example.com"
              autoComplete="email"
              leftIcon={<Mail className="w-4 h-4" />}
              {...patientForm.register('email')}
            />
          </FormField>

          <FormField
            id="phoneNumber"
            label="Phone Number"
            hint="Optional. Used for queue notification SMS and verification."
            error={patientForm.formState.errors.phoneNumber?.message}
          >
            <Input
              type="tel"
              placeholder="+1 (555) 012-3456"
              autoComplete="tel"
              leftIcon={<Phone className="w-4 h-4" />}
              {...patientForm.register('phoneNumber')}
            />
          </FormField>

          <FormField
            id="password"
            label="Password"
            required
            error={patientForm.formState.errors.password?.message}
          >
            <PasswordInput
              placeholder="Create a strong password"
              autoComplete="new-password"
              leftIcon={<Lock className="w-4 h-4" />}
              {...patientForm.register('password')}
            />
          </FormField>

          {/* Password Strength Indicators */}
          <div className="p-3 rounded-lg bg-surface-lavender/40 border border-surface-border text-xs space-y-1">
            <p className="font-medium text-text-primary mb-1">Password requirements:</p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-1 text-text-secondary">
              {passwordChecks.map((check, idx) => (
                <div
                  key={idx}
                  className={`flex items-center gap-1.5 transition-colors ${
                    check.met ? 'text-status-success font-medium' : 'text-text-muted'
                  }`}
                >
                  <CheckCircle2
                    className={`w-3.5 h-3.5 shrink-0 ${
                      check.met ? 'text-status-success' : 'text-text-muted/40'
                    }`}
                  />
                  <span>{check.label}</span>
                </div>
              ))}
            </div>
          </div>

          <FormField
            id="confirmPassword"
            label="Confirm Password"
            required
            error={patientForm.formState.errors.confirmPassword?.message}
          >
            <PasswordInput
              placeholder="Re-enter your password"
              autoComplete="new-password"
              leftIcon={<Lock className="w-4 h-4" />}
              {...patientForm.register('confirmPassword')}
            />
          </FormField>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="md"
              className="w-full"
              isLoading={patientForm.formState.isSubmitting}
            >
              Create Patient Account
            </Button>
          </div>
        </form>
      )}

      {/* DOCTOR FORM */}
      {roleTab === 'DOCTOR' && (
        <form onSubmit={doctorForm.handleSubmit(onDoctorSubmit)} className="space-y-4" noValidate>
          <FormField
            id="docFullName"
            label="Doctor Full Name"
            required
            error={doctorForm.formState.errors.fullName?.message}
          >
            <Input
              placeholder="Dr. Sarah Connor"
              autoComplete="name"
              leftIcon={<User className="w-4 h-4" />}
              {...doctorForm.register('fullName')}
            />
          </FormField>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <FormField
              id="docEmail"
              label="Email Address"
              required
              error={doctorForm.formState.errors.email?.message}
            >
              <Input
                type="email"
                placeholder="dr.sarah@mediqueue.com"
                autoComplete="email"
                leftIcon={<Mail className="w-4 h-4" />}
                {...doctorForm.register('email')}
              />
            </FormField>

            <FormField
              id="docPhoneNumber"
              label="Phone Number"
              error={doctorForm.formState.errors.phoneNumber?.message}
            >
              <Input
                type="tel"
                placeholder="+1 (555) 012-3456"
                autoComplete="tel"
                leftIcon={<Phone className="w-4 h-4" />}
                {...doctorForm.register('phoneNumber')}
              />
            </FormField>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <FormField
              id="docDepartment"
              label="Hospital Department"
              required
              error={doctorForm.formState.errors.departmentId?.message}
            >
              <div className="relative">
                <select
                  id="docDepartment"
                  disabled={loadingDepartments}
                  className="w-full h-10 px-3 py-2 pl-9 bg-surface-card border border-surface-border rounded-xl text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40 focus:border-brand-lavender transition-all"
                  {...doctorForm.register('departmentId')}
                >
                  <option value={0}>Select department...</option>
                  {departments.map((dept) => (
                    <option key={dept.id} value={dept.id}>
                      {dept.name}
                    </option>
                  ))}
                </select>
                <Building2 className="w-4 h-4 text-text-muted absolute left-3 top-3 pointer-events-none" />
              </div>
            </FormField>

            <FormField
              id="docSpecialty"
              label="Specialty"
              required
              error={doctorForm.formState.errors.specialty?.message}
            >
              <Input
                placeholder="e.g. Cardiology, Neurology"
                leftIcon={<Stethoscope className="w-4 h-4" />}
                {...doctorForm.register('specialty')}
              />
            </FormField>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <FormField
              id="docQualification"
              label="Qualification"
              required
              error={doctorForm.formState.errors.qualification?.message}
            >
              <Input
                placeholder="MBBS, MD"
                leftIcon={<GraduationCap className="w-4 h-4" />}
                {...doctorForm.register('qualification')}
              />
            </FormField>

            <FormField
              id="docExperience"
              label="Experience (Yrs)"
              required
              error={doctorForm.formState.errors.yearsOfExperience?.message}
            >
              <Input
                type="number"
                min={0}
                placeholder="5"
                leftIcon={<Briefcase className="w-4 h-4" />}
                {...doctorForm.register('yearsOfExperience')}
              />
            </FormField>

            <FormField
              id="docFee"
              label="Fee ($)"
              required
              error={doctorForm.formState.errors.consultationFee?.message}
            >
              <Input
                type="number"
                min={0}
                step="0.01"
                placeholder="50.00"
                leftIcon={<DollarSign className="w-4 h-4" />}
                {...doctorForm.register('consultationFee')}
              />
            </FormField>
          </div>

          <FormField
            id="docBio"
            label="Professional Bio"
            hint="Optional brief summary for patient discovery."
            error={doctorForm.formState.errors.professionalBio?.message}
          >
            <div className="relative">
              <textarea
                id="docBio"
                rows={2}
                placeholder="Specialist in preventive cardiology with 10+ years hospital experience..."
                className="w-full px-3 py-2 pl-9 bg-surface-card border border-surface-border rounded-xl text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-brand-lavender/40 focus:border-brand-lavender transition-all resize-none"
                {...doctorForm.register('professionalBio')}
              />
              <FileText className="w-4 h-4 text-text-muted absolute left-3 top-3 pointer-events-none" />
            </div>
          </FormField>

          <FormField
            id="docPassword"
            label="Password"
            required
            error={doctorForm.formState.errors.password?.message}
          >
            <PasswordInput
              placeholder="Create a strong practitioner password"
              autoComplete="new-password"
              leftIcon={<Lock className="w-4 h-4" />}
              {...doctorForm.register('password')}
            />
          </FormField>

          {/* Password Strength Indicators */}
          <div className="p-3 rounded-lg bg-surface-lavender/40 border border-surface-border text-xs space-y-1">
            <p className="font-medium text-text-primary mb-1">Password requirements:</p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-1 text-text-secondary">
              {passwordChecks.map((check, idx) => (
                <div
                  key={idx}
                  className={`flex items-center gap-1.5 transition-colors ${
                    check.met ? 'text-status-success font-medium' : 'text-text-muted'
                  }`}
                >
                  <CheckCircle2
                    className={`w-3.5 h-3.5 shrink-0 ${
                      check.met ? 'text-status-success' : 'text-text-muted/40'
                    }`}
                  />
                  <span>{check.label}</span>
                </div>
              ))}
            </div>
          </div>

          <FormField
            id="docConfirmPassword"
            label="Confirm Password"
            required
            error={doctorForm.formState.errors.confirmPassword?.message}
          >
            <PasswordInput
              placeholder="Re-enter your password"
              autoComplete="new-password"
              leftIcon={<Lock className="w-4 h-4" />}
              {...doctorForm.register('confirmPassword')}
            />
          </FormField>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="md"
              className="w-full"
              isLoading={doctorForm.formState.isSubmitting}
            >
              Create Doctor Account
            </Button>
          </div>
        </form>
      )}

      <div className="pt-4 border-t border-surface-border text-center mt-4">
        <p className="text-sm text-text-secondary">
          Already have an account?{' '}
          <Link
            to="/login"
            className="text-brand-lavender font-semibold hover:underline focus-visible:outline-none focus-visible-ring rounded"
          >
            Sign In
          </Link>
        </p>
      </div>
    </AuthLayout>
  );
};
