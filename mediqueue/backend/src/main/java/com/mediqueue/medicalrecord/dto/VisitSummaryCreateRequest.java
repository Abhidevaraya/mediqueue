package com.mediqueue.medicalrecord.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitSummaryCreateRequest {

    @NotNull(message = "Consultation ID is required")
    private Long consultationId;

    @Size(max = 500, message = "Chief complaint cannot exceed 500 characters")
    private String chiefComplaint;

    @Size(max = 5000, message = "Diagnosis cannot exceed 5000 characters")
    private String diagnosis;

    @Size(max = 10000, message = "Clinical notes cannot exceed 10000 characters")
    private String clinicalNotes;

    @Size(max = 5000, message = "Treatment plan cannot exceed 5000 characters")
    private String treatmentPlan;
}
