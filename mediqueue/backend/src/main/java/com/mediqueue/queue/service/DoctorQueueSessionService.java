package com.mediqueue.queue.service;

import com.mediqueue.queue.repository.DoctorQueueSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DoctorQueueSessionService {

    private final DoctorQueueSessionRepository sessionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureSessionExists(Long doctorId, LocalDate queueDate) {
        sessionRepository.insertIgnoreDoctorQueueSession(doctorId, queueDate);
    }
}
