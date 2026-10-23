import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { doctorService } from '@/lib/api/doctorService';
import { DoctorPublicResponse } from '@/types/doctor';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Spinner } from '@/components/ui/Spinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { AppointmentBookingModal } from '@/features/appointments/AppointmentBookingModal';
import {
  Search,
  Filter,
  User,
  GraduationCap,
  Award,
  DollarSign,
  Calendar,
  Building,
} from 'lucide-react';

export const DoctorDirectoryPage: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDeptId, setSelectedDeptId] = useState<number | undefined>(undefined);
  const [selectedDoctor, setSelectedDoctor] = useState<DoctorPublicResponse | null>(null);
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);

  // Departments query
  const { data: deptPage } = useQuery({
    queryKey: ['departments'],
    queryFn: () => doctorService.getDepartments(0, 100),
  });

  // Doctors query
  const {
    data: doctorPage,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['doctors', selectedDeptId],
    queryFn: () => doctorService.getDoctors({ departmentId: selectedDeptId, size: 50 }),
  });

  const doctors = doctorPage?.content || [];

  // Client-side search filtering by name or specialty
  const filteredDoctors = doctors.filter((doc) => {
    const matchesSearch =
      doc.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      doc.specialty.toLowerCase().includes(searchTerm.toLowerCase()) ||
      doc.departmentName.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesSearch;
  });

  const handleOpenBooking = (doc: DoctorPublicResponse) => {
    setSelectedDoctor(doc);
    setIsBookingModalOpen(true);
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-text-primary tracking-tight font-heading">
          Find a Specialist
        </h1>
        <p className="text-sm text-text-secondary mt-1">
          Browse verified hospital physicians, check availability schedules, and reserve appointment time slots.
        </p>
      </div>

      {/* Filter and Search Bar */}
      <Card className="border-surface-border">
        <CardContent className="p-4 sm:p-5">
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            {/* Search Input */}
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-text-muted absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                placeholder="Search by doctor name, specialty, or department..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full h-11 pl-10 pr-4 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender focus:ring-2 focus:ring-brand-lavender/20"
              />
            </div>

            {/* Department Filter Dropdown */}
            <div className="relative min-w-[200px]">
              <Filter className="w-4 h-4 text-text-muted absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <select
                value={selectedDeptId ?? ''}
                onChange={(e) =>
                  setSelectedDeptId(e.target.value ? Number(e.target.value) : undefined)
                }
                className="w-full h-11 pl-10 pr-8 rounded-xl text-sm bg-surface-elevated text-text-primary border border-surface-border focus:outline-none focus:border-brand-lavender focus:ring-2 focus:ring-brand-lavender/20 appearance-none cursor-pointer"
              >
                <option value="">All Departments</option>
                {deptPage?.content.map((dept) => (
                  <option key={dept.id} value={dept.id}>
                    {dept.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Doctor Cards Grid */}
      {isLoading ? (
        <div className="p-16 flex flex-col items-center justify-center gap-3">
          <Spinner size="lg" className="text-brand-lavender" />
          <p className="text-sm font-medium text-text-secondary">Loading doctor directory...</p>
        </div>
      ) : isError ? (
        <ErrorState
          title="Could not load doctors"
          message="Failed to retrieve verified clinician directory from the server."
          onRetry={() => refetch()}
        />
      ) : filteredDoctors.length === 0 ? (
        <EmptyState
          title="No doctors found"
          description={
            searchTerm || selectedDeptId
              ? 'No physicians match your filter criteria. Try clearing filters or searching for a different specialty.'
              : 'No doctors are currently registered or verified in the hospital directory.'
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredDoctors.map((doc) => (
            <Card
              key={doc.id}
              className="border-surface-border flex flex-col justify-between hover:shadow-soft-md transition-all duration-200"
            >
              <CardContent className="p-6 space-y-4">
                <div className="flex items-start gap-4">
                  <div className="w-14 h-14 rounded-2xl bg-brand-surface text-brand-lavender flex items-center justify-center font-bold text-lg shrink-0">
                    <User className="w-7 h-7" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <h2 className="font-bold text-text-primary text-base leading-snug truncate">
                      {doc.fullName}
                    </h2>
                    <p className="text-xs font-semibold text-brand-lavender mt-0.5">{doc.specialty}</p>
                    <div className="flex items-center gap-1.5 text-xs text-text-secondary mt-1 truncate">
                      <Building className="w-3.5 h-3.5 shrink-0" />
                      <span>{doc.departmentName}</span>
                    </div>
                  </div>
                </div>

                {doc.professionalBio && (
                  <p className="text-xs text-text-secondary line-clamp-2 leading-relaxed">
                    {doc.professionalBio}
                  </p>
                )}

                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-surface-border text-xs">
                  <div className="flex items-center gap-1.5 text-text-secondary">
                    <Award className="w-3.5 h-3.5 text-brand-lavender" />
                    <span>{doc.yearsOfExperience} yrs exp</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-text-secondary">
                    <DollarSign className="w-3.5 h-3.5 text-status-success" />
                    <span>${doc.consultationFee} fee</span>
                  </div>
                  <div className="col-span-2 flex items-center gap-1.5 text-text-muted truncate">
                    <GraduationCap className="w-3.5 h-3.5 shrink-0" />
                    <span className="truncate">{doc.qualification}</span>
                  </div>
                </div>

                <div className="pt-2">
                  <Button
                    variant="primary"
                    size="sm"
                    className="w-full"
                    leftIcon={<Calendar className="w-4 h-4" />}
                    onClick={() => handleOpenBooking(doc)}
                  >
                    Check Slots & Book
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Booking Modal */}
      <AppointmentBookingModal
        doctor={selectedDoctor}
        isOpen={isBookingModalOpen}
        onClose={() => setIsBookingModalOpen(false)}
      />
    </div>
  );
};
