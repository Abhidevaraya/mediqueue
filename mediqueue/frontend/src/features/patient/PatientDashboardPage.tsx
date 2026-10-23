import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '@/lib/auth/AuthContext';
import { useToast } from '@/lib/feedback/ToastContext';
import { appointmentService } from '@/lib/api/appointmentService';
import { queueService } from '@/lib/api/queueService';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Spinner } from '@/components/ui/Spinner';
import {
  Calendar,
  Clock,
  Activity,
  ArrowRight,
  FileText,
  UserCheck,
  Users,
} from 'lucide-react';

export const PatientDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();

  // Active queue status query
  const { data: activeQueue, isLoading: isQueueLoading } = useQuery({
    queryKey: ['queue', 'my-status'],
    queryFn: () => queueService.getMyActiveQueueStatus(),
    retry: false,
  });

  // Active queue wait time prediction query
  const { data: waitPrediction } = useQuery({
    queryKey: ['queue', 'my-wait-estimate'],
    queryFn: () => queueService.getMyWaitTimeEstimate(),
    enabled: Boolean(activeQueue?.id),
    retry: false,
  });

  // Upcoming appointments query
  const { data: appointmentPage, isLoading: isAppointmentsLoading } = useQuery({
    queryKey: ['appointments', 'my', 'BOOKED'],
    queryFn: () => appointmentService.getMyAppointments('BOOKED', 0, 5),
  });

  // Check-in mutation
  const checkInMutation = useMutation({
    mutationFn: queueService.checkInAppointment,
    onSuccess: (queueEntry) => {
      toast.success(
        `Checked in successfully! Your Token Number is #${queueEntry.tokenNumber}`,
        'Checked In'
      );
      queryClient.invalidateQueries({ queryKey: ['queue'] });
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
    },
    onError: (err: any) => {
      toast.error(err?.message || 'Check-in failed. Please verify appointment eligibility.');
    },
  });

  const appointments = appointmentPage?.content || [];

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-surface-border">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-text-primary tracking-tight font-heading">
            Welcome back, {user?.fullName}!
          </h1>
          <p className="text-sm text-text-secondary mt-1">
            Manage your consultations, monitor live queue tokens, and review clinical records.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link to="/patient/doctors">
            <Button variant="primary" size="md" leftIcon={<Calendar className="w-4 h-4" />}>
              Book Consultation
            </Button>
          </Link>
        </div>
      </div>

      {/* Active Live Queue Token Banner (If checked in) */}
      {isQueueLoading ? (
        <Card className="border-surface-border">
          <CardContent className="p-6 flex items-center justify-center gap-2">
            <Spinner size="sm" className="text-brand-lavender" />
            <span className="text-xs text-text-secondary">Checking for active queue token...</span>
          </CardContent>
        </Card>
      ) : activeQueue ? (
        <Card className="border-brand-lavender/30 bg-gradient-to-r from-brand-surface/60 to-surface-card overflow-hidden shadow-soft-md">
          <CardContent className="p-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <Badge variant="primary" size="md" className="gap-1.5 animate-pulse">
                    <Activity className="w-3.5 h-3.5" />
                    <span>Live Queue Active Today</span>
                  </Badge>
                  <Badge variant="success" size="sm">
                    Status: {activeQueue.status}
                  </Badge>
                </div>

                <div className="flex items-baseline gap-3">
                  <span className="text-4xl sm:text-5xl font-extrabold text-brand-deep dark:text-brand-lavender font-heading">
                    #{activeQueue.tokenNumber}
                  </span>
                  <div className="text-xs text-text-secondary">
                    <p className="font-semibold text-text-primary">{activeQueue.doctorName}</p>
                    <p>{activeQueue.departmentName}</p>
                  </div>
                </div>

                {waitPrediction && (
                  <div className="flex flex-wrap items-center gap-4 text-xs text-text-secondary pt-1">
                    <div className="flex items-center gap-1.5 font-medium text-brand-deep dark:text-surface-card">
                      <Clock className="w-4 h-4 text-brand-lavender" />
                      <span>Estimated Wait: ~{waitPrediction.estimatedWaitMinutes} mins</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Users className="w-4 h-4 text-brand-lavender" />
                      <span>{waitPrediction.eligiblePatientsAhead} patients ahead</span>
                    </div>
                  </div>
                )}
              </div>

              <div className="flex items-center gap-3 shrink-0">
                <Link to="/patient/queue">
                  <Button
                    variant="primary"
                    size="lg"
                    className="shadow-md shadow-brand-lavender/25"
                    rightIcon={<ArrowRight className="w-4 h-4" />}
                  >
                    View Live Tracker
                  </Button>
                </Link>
              </div>
            </div>
          </CardContent>
        </Card>
      ) : null}

      {/* Grid of Quick Actions & Upcoming Appointments */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upcoming Appointments (2 Columns) */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-text-primary tracking-tight font-heading">
              Upcoming Appointments
            </h2>
            <Link to="/patient/appointments" className="text-xs font-semibold text-brand-lavender hover:underline">
              View All
            </Link>
          </div>

          {isAppointmentsLoading ? (
            <Card className="border-surface-border">
              <CardContent className="p-8 text-center">
                <Spinner size="md" className="text-brand-lavender mx-auto mb-2" />
                <p className="text-xs text-text-secondary">Loading appointments...</p>
              </CardContent>
            </Card>
          ) : appointments.length === 0 ? (
            <Card className="border-surface-border">
              <CardContent className="p-8 text-center space-y-3">
                <Calendar className="w-10 h-10 text-text-muted mx-auto opacity-50" />
                <div>
                  <h3 className="font-bold text-text-primary text-sm">No booked consultations</h3>
                  <p className="text-xs text-text-secondary mt-1 max-w-sm mx-auto">
                    You have no upcoming consultations scheduled. Discover specialists and book an appointment online.
                  </p>
                </div>
                <Link to="/patient/doctors">
                  <Button variant="secondary" size="sm" className="mt-2">
                    Browse Specialists
                  </Button>
                </Link>
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-3">
              {appointments.map((appt) => {
                const apptDate = new Date(appt.startDateTime);
                const isToday =
                  new Date().toISOString().split('T')[0] ===
                  appt.startDateTime.split('T')[0];

                return (
                  <Card key={appt.id} className="border-surface-border hover:shadow-soft-sm transition-shadow">
                    <CardContent className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div className="flex items-start gap-3.5">
                        <div className="w-12 h-12 rounded-xl bg-brand-surface text-brand-lavender flex flex-col items-center justify-center shrink-0">
                          <span className="text-[10px] font-bold uppercase leading-none">
                            {apptDate.toLocaleDateString('en-US', { month: 'short' })}
                          </span>
                          <span className="text-base font-extrabold leading-none mt-0.5">
                            {apptDate.getDate()}
                          </span>
                        </div>
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <h3 className="font-bold text-text-primary text-sm truncate">{appt.doctorName}</h3>
                            <Badge variant="primary" size="sm">
                              {appt.status}
                            </Badge>
                          </div>
                          <p className="text-xs text-text-secondary mt-0.5">
                            {appt.specialty} • {appt.departmentName}
                          </p>
                          <div className="flex items-center gap-3 text-xs text-text-muted mt-1.5">
                            <span className="flex items-center gap-1">
                              <Clock className="w-3.5 h-3.5" />
                              {apptDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </span>
                            <span>Ref: #{appt.appointmentReference}</span>
                          </div>
                        </div>
                      </div>

                      {/* Action buttons */}
                      <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                        {isToday && appt.status === 'BOOKED' && !activeQueue && (
                          <Button
                            variant="primary"
                            size="sm"
                            isLoading={checkInMutation.isPending}
                            onClick={() => checkInMutation.mutate(appt.id)}
                            leftIcon={<UserCheck className="w-4 h-4" />}
                          >
                            Check In
                          </Button>
                        )}
                        <Link to={`/patient/appointments`}>
                          <Button variant="ghost" size="sm">
                            Details
                          </Button>
                        </Link>
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          )}
        </div>

        {/* Quick Navigation Cards (1 Column) */}
        <div className="space-y-4">
          <h2 className="text-lg font-bold text-text-primary tracking-tight font-heading">
            Quick Navigation
          </h2>

          <div className="space-y-3">
            <Link to="/patient/doctors" className="block group">
              <Card className="border-surface-border group-hover:border-brand-lavender/40 transition-colors">
                <CardContent className="p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-brand-surface text-brand-lavender flex items-center justify-center">
                      <Calendar className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-text-primary">Find a Specialist</h3>
                      <p className="text-xs text-text-secondary">Search verified doctor directory</p>
                    </div>
                  </div>
                  <ArrowRight className="w-4 h-4 text-text-muted group-hover:text-brand-lavender transition-colors" />
                </CardContent>
              </Card>
            </Link>

            <Link to="/patient/queue" className="block group">
              <Card className="border-surface-border group-hover:border-brand-lavender/40 transition-colors">
                <CardContent className="p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-brand-surface text-brand-lavender flex items-center justify-center">
                      <Activity className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-text-primary">Live Queue Tracker</h3>
                      <p className="text-xs text-text-secondary">Monitor position and waiting times</p>
                    </div>
                  </div>
                  <ArrowRight className="w-4 h-4 text-text-muted group-hover:text-brand-lavender transition-colors" />
                </CardContent>
              </Card>
            </Link>

            <Link to="/patient/records" className="block group">
              <Card className="border-surface-border group-hover:border-brand-lavender/40 transition-colors">
                <CardContent className="p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-brand-surface text-brand-lavender flex items-center justify-center">
                      <FileText className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-text-primary">Clinical History & Rx</h3>
                      <p className="text-xs text-text-secondary">Visit summaries and prescriptions</p>
                    </div>
                  </div>
                  <ArrowRight className="w-4 h-4 text-text-muted group-hover:text-brand-lavender transition-colors" />
                </CardContent>
              </Card>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
