import React from 'react';
import { Link } from 'react-router-dom';
import { Activity, ShieldCheck, Clock, CheckCircle } from 'lucide-react';
import { ThemeToggle } from '@/components/layout/ThemeToggle';

export interface AuthLayoutProps {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}

export const AuthLayout: React.FC<AuthLayoutProps> = ({ title, subtitle, children }) => {
  return (
    <div className="min-h-screen flex flex-col justify-between bg-surface transition-colors">
      {/* Top Bar */}
      <header className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2.5 select-none">
          <div className="w-9 h-9 rounded-xl bg-brand-lavender flex items-center justify-center text-white shadow-soft-sm">
            <Activity className="w-5 h-5" />
          </div>
          <span className="text-lg font-bold font-display text-brand-indigo dark:text-white">
            MediQueue
          </span>
        </Link>
        <ThemeToggle />
      </header>

      {/* Main Container */}
      <main className="flex-1 flex items-center justify-center p-4 sm:p-6 lg:p-8">
        <div className="w-full max-w-5xl grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          {/* Left Hero Column (Large Screens) */}
          <div className="hidden lg:flex lg:col-span-5 flex-col gap-6 pr-4">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-brand-lavender-light text-brand-indigo dark:bg-brand-lavender/20 dark:text-brand-lavender text-xs font-semibold w-fit">
              <ShieldCheck className="w-4 h-4 text-brand-lavender" />
              <span>Verified Hospital Healthcare</span>
            </div>

            <h1 className="text-3xl font-extrabold font-display text-brand-indigo dark:text-white leading-tight">
              Hospital Queue & Appointment Intelligence
            </h1>

            <p className="text-sm text-text-secondary leading-relaxed">
              Experience transparent, digital patient care with real-time queue tokens, dynamic waiting-time forecasts, and verified clinical appointments.
            </p>

            <div className="space-y-3 pt-2">
              <div className="flex items-center gap-3 text-sm text-text-primary">
                <CheckCircle className="w-4 h-4 text-status-success shrink-0" />
                <span>Zero guesswork with live token tracking</span>
              </div>
              <div className="flex items-center gap-3 text-sm text-text-primary">
                <CheckCircle className="w-4 h-4 text-status-success shrink-0" />
                <span>Encrypted medical records & prescriptions</span>
              </div>
              <div className="flex items-center gap-3 text-sm text-text-primary">
                <Clock className="w-4 h-4 text-brand-lavender shrink-0" />
                <span>Historical EMA wait-time predictions</span>
              </div>
            </div>
          </div>

          {/* Right Form Card */}
          <div className="lg:col-span-7 flex justify-center">
            <div className="w-full max-w-md bg-surface-elevated rounded-3xl border border-surface-border p-6 sm:p-8 shadow-soft-lg transition-all">
              <div className="mb-6 text-center sm:text-left">
                <h2 className="text-2xl font-bold font-display text-text-primary tracking-tight">
                  {title}
                </h2>
                <p className="text-sm text-text-secondary mt-1">{subtitle}</p>
              </div>

              {children}
            </div>
          </div>
        </div>
      </main>

      {/* Mini Footer */}
      <footer className="py-4 text-center text-xs text-text-muted">
        © {new Date().getFullYear()} MediQueue Hospital Platform. All rights reserved.
      </footer>
    </div>
  );
};
