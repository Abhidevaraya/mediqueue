import React from 'react';
import { Link } from 'react-router-dom';
import { Navbar } from '@/components/layout/Navbar';
import { Footer } from '@/components/layout/Footer';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  Calendar,
  Clock,
  Activity,
  ShieldCheck,
  Bell,
  FileText,
  Users,
  ChevronRight,
  ArrowRight,
  Sparkles,
  CheckCircle,
} from 'lucide-react';

export const LandingPage: React.FC = () => {
  const { isAuthenticated, user } = useAuth();

  const getDashboardPath = () => {
    if (!user) return '/patient';
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
    <div className="min-h-screen flex flex-col bg-brand-bg text-text-primary transition-colors">
      <Navbar />

      <main className="flex-1">
        {/* HERO SECTION */}
        <section className="relative overflow-hidden pt-12 pb-20 md:pt-20 md:pb-28">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
              
              {/* Left Column: Headline and CTAs */}
              <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
                <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-brand-surface border border-brand-lavender/30 text-brand-deep font-medium text-xs sm:text-sm">
                  <Sparkles className="w-4 h-4 text-brand-lavender" />
                  <span>Smart Hospital Queuing & Appointment Orchestration</span>
                </div>

                <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-brand-deep dark:text-surface-card leading-[1.15]">
                  Ditch the Waiting Room.{' '}
                  <span className="text-transparent bg-clip-text bg-gradient-to-r from-brand-lavender to-brand-deep">
                    Real-Time Care,
                  </span>{' '}
                  On Your Schedule.
                </h1>

                <p className="text-base sm:text-lg text-text-secondary max-w-2xl mx-auto lg:mx-0 leading-relaxed">
                  MediQueue eliminates stressful clinic delays through intelligent queue forecasting,
                  verified doctor scheduling, and live token notifications directly to your phone.
                </p>

                <div className="pt-2 flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-3 sm:gap-4">
                  {isAuthenticated ? (
                    <Link to={getDashboardPath()}>
                      <Button variant="primary" size="lg" rightIcon={<ArrowRight className="w-4 h-4" />}>
                        Go to My Dashboard
                      </Button>
                    </Link>
                  ) : (
                    <>
                      <Link to="/register" className="w-full sm:w-auto">
                        <Button
                          variant="primary"
                          size="lg"
                          className="w-full sm:w-auto shadow-md shadow-brand-lavender/25"
                          rightIcon={<ArrowRight className="w-4 h-4" />}
                        >
                          Book an Appointment
                        </Button>
                      </Link>
                      <a href="#how-it-works" className="w-full sm:w-auto">
                        <Button variant="secondary" size="lg" className="w-full sm:w-auto">
                          How MediQueue Works
                        </Button>
                      </a>
                    </>
                  )}
                </div>

                {/* Micro trust indicators */}
                <div className="pt-4 flex flex-wrap items-center justify-center lg:justify-start gap-y-2 gap-x-6 text-xs text-text-secondary">
                  <div className="flex items-center gap-1.5">
                    <CheckCircle className="w-4 h-4 text-status-success shrink-0" />
                    <span>Live algorithmic queue updates</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <CheckCircle className="w-4 h-4 text-status-success shrink-0" />
                    <span>Zero crowded waiting rooms</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <CheckCircle className="w-4 h-4 text-status-success shrink-0" />
                    <span>Direct physician consultation history</span>
                  </div>
                </div>
              </div>

              {/* Right Column: Live Interface Composition Preview */}
              <div className="lg:col-span-5 relative">
                <div className="relative mx-auto max-w-md w-full">
                  {/* Outer glow accent */}
                  <div className="absolute -inset-1.5 bg-gradient-to-r from-brand-lavender/30 to-brand-deep/20 rounded-3xl blur-xl opacity-70" />

                  {/* Synthetic Interactive Card Preview */}
                  <Card className="relative shadow-xl border-surface-border overflow-hidden">
                    {/* Header bar */}
                    <div className="bg-brand-deep text-white px-5 py-4 flex items-center justify-between">
                      <div className="flex items-center gap-2.5">
                        <div className="w-3 h-3 rounded-full bg-status-success animate-pulse" />
                        <span className="font-semibold text-sm tracking-wide">Live Queue Tracker</span>
                      </div>
                      <Badge variant="neutral" size="sm" className="bg-white/10 text-white border-white/20">
                        Room 304 • Cardiology
                      </Badge>
                    </div>

                    <CardContent className="p-6 space-y-5">
                      {/* Token Hero Display */}
                      <div className="p-4 rounded-2xl bg-surface-lavender/40 border border-surface-border text-center space-y-1">
                        <span className="text-xs uppercase tracking-wider font-semibold text-text-secondary">
                          Your Active Token
                        </span>
                        <div className="text-4xl font-extrabold text-brand-deep dark:text-brand-lavender font-heading tracking-tight">
                          #CARD-042
                        </div>
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-status-success-surface text-status-success font-medium text-xs">
                          <Activity className="w-3.5 h-3.5" />
                          <span>Status: In Consultation</span>
                        </div>
                      </div>

                      {/* Doctor info */}
                      <div className="flex items-center gap-3 p-3 rounded-xl bg-surface-card border border-surface-border">
                        <div className="w-10 h-10 rounded-full bg-brand-surface flex items-center justify-center text-brand-lavender font-bold text-sm">
                          DJ
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-semibold text-text-primary truncate">Dr. Diana Jenkins, MD</p>
                          <p className="text-xs text-text-secondary">Senior Cardiologist • Dept of Medicine</p>
                        </div>
                      </div>

                      {/* Wait time metric cards */}
                      <div className="grid grid-cols-2 gap-3">
                        <div className="p-3 rounded-xl bg-surface-lavender/30 border border-surface-border text-center">
                          <div className="flex items-center justify-center gap-1 text-xs text-text-secondary mb-0.5">
                            <Clock className="w-3.5 h-3.5 text-brand-lavender" />
                            <span>Estimated Wait</span>
                          </div>
                          <span className="text-lg font-bold text-brand-deep dark:text-surface-card">
                            ~8 mins
                          </span>
                        </div>
                        <div className="p-3 rounded-xl bg-surface-lavender/30 border border-surface-border text-center">
                          <div className="flex items-center justify-center gap-1 text-xs text-text-secondary mb-0.5">
                            <Users className="w-3.5 h-3.5 text-brand-lavender" />
                            <span>Ahead of You</span>
                          </div>
                          <span className="text-lg font-bold text-brand-deep dark:text-surface-card">
                            1 Patient
                          </span>
                        </div>
                      </div>

                      {/* Queue timeline stepper preview */}
                      <div className="space-y-2 pt-1 border-t border-surface-border">
                        <div className="flex items-center justify-between text-xs text-text-secondary">
                          <span>Queue Progression</span>
                          <span className="font-semibold text-brand-lavender">85% Complete</span>
                        </div>
                        <div className="w-full h-2 rounded-full bg-surface-border overflow-hidden">
                          <div className="h-full bg-brand-lavender rounded-full transition-all duration-500 w-[85%]" />
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              </div>

            </div>
          </div>
        </section>

        {/* CORE CAPABILITIES GRID */}
        <section className="py-16 md:py-24 bg-surface-card border-y border-surface-border">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-3xl mx-auto mb-14 space-y-3">
              <Badge variant="primary" size="md">Platform Features</Badge>
              <h2 className="text-3xl sm:text-4xl font-bold text-brand-deep dark:text-surface-card tracking-tight">
                Designed for Patient Comfort and Clinical Efficiency
              </h2>
              <p className="text-text-secondary text-base sm:text-lg">
                Every tool built into MediQueue is structured to save time, reduce anxiety, and keep care teams coordinated.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
              {/* Feature 1 */}
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="p-6 space-y-3">
                  <div className="w-12 h-12 rounded-xl bg-brand-surface flex items-center justify-center text-brand-lavender">
                    <Clock className="w-6 h-6" />
                  </div>
                  <h3 className="text-lg font-bold text-text-primary">Real-Time Queue Tracking</h3>
                  <p className="text-sm text-text-secondary leading-relaxed">
                    Watch your position update dynamically via WebSocket STOMP messaging. Know precisely when your turn approaches.
                  </p>
                </CardContent>
              </Card>

              {/* Feature 2 */}
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="p-6 space-y-3">
                  <div className="w-12 h-12 rounded-xl bg-brand-surface flex items-center justify-center text-brand-lavender">
                    <Calendar className="w-6 h-6" />
                  </div>
                  <h3 className="text-lg font-bold text-text-primary">Specialty-Based Booking</h3>
                  <p className="text-sm text-text-secondary leading-relaxed">
                    Browse departments, verify practitioner schedules, and secure available consultation time slots without phone queues.
                  </p>
                </CardContent>
              </Card>

              {/* Feature 3 */}
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="p-6 space-y-3">
                  <div className="w-12 h-12 rounded-xl bg-brand-surface flex items-center justify-center text-brand-lavender">
                    <FileText className="w-6 h-6" />
                  </div>
                  <h3 className="text-lg font-bold text-text-primary">Consultation Records & Rx</h3>
                  <p className="text-sm text-text-secondary leading-relaxed">
                    Review clinician summaries, e-prescriptions, and follow-up guidance in one secure, unified medical history timeline.
                  </p>
                </CardContent>
              </Card>

              {/* Feature 4 */}
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="p-6 space-y-3">
                  <div className="w-12 h-12 rounded-xl bg-brand-surface flex items-center justify-center text-brand-lavender">
                    <Bell className="w-6 h-6" />
                  </div>
                  <h3 className="text-lg font-bold text-text-primary">Timely Notification Dispatch</h3>
                  <p className="text-sm text-text-secondary leading-relaxed">
                    Receive alerts when your token is called, when consultation rooms change, or when your physician begins rounds.
                  </p>
                </CardContent>
              </Card>

              {/* Feature 5 */}
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="p-6 space-y-3">
                  <div className="w-12 h-12 rounded-xl bg-brand-surface flex items-center justify-center text-brand-lavender">
                    <Users className="w-6 h-6" />
                  </div>
                  <h3 className="text-lg font-bold text-text-primary">Doctor & OPD Station Tools</h3>
                  <p className="text-sm text-text-secondary leading-relaxed">
                    Dedicated doctor interfaces allow clinical staff to call next patients, pause sessions, and log visit notes seamlessly.
                  </p>
                </CardContent>
              </Card>

              {/* Feature 6 */}
              <Card className="hover:shadow-md transition-shadow">
                <CardContent className="p-6 space-y-3">
                  <div className="w-12 h-12 rounded-xl bg-brand-surface flex items-center justify-center text-brand-lavender">
                    <ShieldCheck className="w-6 h-6" />
                  </div>
                  <h3 className="text-lg font-bold text-text-primary">Role-Based Security</h3>
                  <p className="text-sm text-text-secondary leading-relaxed">
                    Strict role segregation between patients, practitioners, and administrators ensures confidentiality and integrity.
                  </p>
                </CardContent>
              </Card>
            </div>
          </div>
        </section>

        {/* WORKFLOW EXPLANATION: HOW IT WORKS */}
        <section id="how-it-works" className="py-16 md:py-24">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-2xl mx-auto mb-14 space-y-3">
              <Badge variant="neutral" size="md">Simple Workflow</Badge>
              <h2 className="text-3xl sm:text-4xl font-bold text-brand-deep dark:text-surface-card tracking-tight">
                From Booking to Consultation in 4 Steps
              </h2>
              <p className="text-text-secondary text-base">
                How our integrated queueing pipeline turns clinical visits into smooth experiences.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {[
                {
                  step: '01',
                  title: 'Book Appointment',
                  desc: 'Pick your specialty, clinician, and preferred available time slot with instant confirmation.',
                },
                {
                  step: '02',
                  title: 'Receive Digital Token',
                  desc: 'On your appointment day, an automated smart queue token is generated for your consultation.',
                },
                {
                  step: '03',
                  title: 'Track Live Waiting',
                  desc: 'Monitor wait forecasts and position in line from your smartphone without sitting in waiting rooms.',
                },
                {
                  step: '04',
                  title: 'Consult & Review',
                  desc: 'Meet your doctor when called and immediately access consultation notes and digital prescriptions.',
                },
              ].map((item, idx) => (
                <div
                  key={idx}
                  className="relative p-6 rounded-2xl bg-surface-card border border-surface-border space-y-3"
                >
                  <div className="text-3xl font-extrabold text-brand-lavender/40 font-heading">
                    {item.step}
                  </div>
                  <h3 className="text-lg font-bold text-text-primary">{item.title}</h3>
                  <p className="text-sm text-text-secondary leading-relaxed">{item.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* CALL TO ACTION BANNER */}
        <section className="py-12 bg-gradient-to-r from-brand-deep to-brand-deep/95 text-white">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex flex-col md:flex-row items-center justify-between gap-6 text-center md:text-left">
              <div className="space-y-2">
                <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">
                  Ready for a smoother hospital consultation?
                </h2>
                <p className="text-surface-lavender/80 text-sm sm:text-base max-w-xl">
                  Sign up in seconds as a patient or log into your existing account to view upcoming visits.
                </p>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <Link to="/register">
                  <Button
                    variant="primary"
                    size="lg"
                    className="bg-brand-lavender hover:bg-brand-lavender-hover text-white shadow-lg"
                    rightIcon={<ChevronRight className="w-4 h-4" />}
                  >
                    Get Started Free
                  </Button>
                </Link>
                <Link to="/login">
                  <Button
                    variant="secondary"
                    size="lg"
                    className="bg-white/10 hover:bg-white/20 text-white border-white/20"
                  >
                    Sign In
                  </Button>
                </Link>
              </div>
            </div>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
};
