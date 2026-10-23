package com.mediqueue.queue.dto;

import com.mediqueue.queue.entity.DoctorQueueSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueSessionResponse {

    private Long id;
    private Long doctorId;
    private LocalDate queueDate;
    private Integer lastTokenNumber;
    private Boolean isPaused;
    private Instant pausedAt;
    private String pauseReason;

    public static QueueSessionResponse from(DoctorQueueSession session) {
        return QueueSessionResponse.builder()
                .id(session.getId())
                .doctorId(session.getDoctor().getId())
                .queueDate(session.getQueueDate())
                .lastTokenNumber(session.getLastTokenNumber())
                .isPaused(session.getIsPaused())
                .pausedAt(session.getPausedAt())
                .pauseReason(session.getPauseReason())
                .build();
    }
}
