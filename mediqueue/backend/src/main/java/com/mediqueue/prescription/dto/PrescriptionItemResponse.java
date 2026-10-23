package com.mediqueue.prescription.dto;

import com.mediqueue.prescription.entity.PrescriptionItem;
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
public class PrescriptionItemResponse {

    private Long id;
    private String medicationName;
    private String dosage;
    private String frequency;
    private Integer durationDays;
    private String route;
    private String instructions;
    private Integer itemOrder;

    public static PrescriptionItemResponse from(PrescriptionItem item) {
        return PrescriptionItemResponse.builder()
                .id(item.getId())
                .medicationName(item.getMedicationName())
                .dosage(item.getDosage())
                .frequency(item.getFrequency())
                .durationDays(item.getDurationDays())
                .route(item.getRoute())
                .instructions(item.getInstructions())
                .itemOrder(item.getItemOrder())
                .build();
    }
}
