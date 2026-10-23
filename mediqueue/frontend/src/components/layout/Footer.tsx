import React from 'react';
import { Activity, ShieldCheck, Clock, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';

export const Footer: React.FC = () => {
  return (
    <footer className="w-full bg-surface-elevated border-t border-surface-border mt-auto transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          {/* Brand info */}
          <div className="md:col-span-2 space-y-3">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-brand-lavender flex items-center justify-center text-white">
                <Activity className="w-4 h-4" />
              </div>
              <span className="text-base font-bold font-display text-brand-indigo dark:text-white">
                MediQueue
              </span>
            </div>
            <p className="text-sm text-text-secondary max-w-sm leading-relaxed">
              Smart Hospital Appointment Scheduling & Real-Time Queue Management Platform. Powered by verified doctor schedules, tokenized queues, and dynamic wait-time estimation.
            </p>
          </div>

          {/* Quick Links */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-text-muted font-display">
              Platform Features
            </h4>
            <ul className="space-y-2 text-sm text-text-secondary">
              <li className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-brand-lavender" />
                <span>Live Digital Queue</span>
              </li>
              <li className="flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-status-success" />
                <span>Verified Clinicians</span>
              </li>
              <li className="flex items-center gap-2">
                <FileText className="w-4 h-4 text-brand-lavender" />
                <span>Electronic Medical Records</span>
              </li>
            </ul>
          </div>

          {/* Portals */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-text-muted font-display">
              Access Portals
            </h4>
            <ul className="space-y-2 text-sm text-text-secondary">
              <li>
                <Link to="/login" className="hover:text-brand-lavender transition-colors">
                  Patient Portal
                </Link>
              </li>
              <li>
                <Link to="/login" className="hover:text-brand-lavender transition-colors">
                  Doctor Clinical Workspace
                </Link>
              </li>
              <li>
                <Link to="/login" className="hover:text-brand-lavender transition-colors">
                  Hospital Administration
                </Link>
              </li>
            </ul>
          </div>
        </div>

        <div className="pt-8 border-t border-surface-border/60 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-text-muted">
          <p>© {new Date().getFullYear()} MediQueue. All rights reserved.</p>
          <p className="text-center sm:text-right">
            Demonstration system built with Spring Boot 3 & React. Synthetic clinical data only.
          </p>
        </div>
      </div>
    </footer>
  );
};
