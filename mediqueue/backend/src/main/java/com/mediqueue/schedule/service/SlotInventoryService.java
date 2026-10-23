package com.mediqueue.schedule.service;

import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.schedule.entity.SlotInventory;
import com.mediqueue.schedule.repository.SlotInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SlotInventoryService {

    private final SlotInventoryRepository slotInventoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureSlotInventoryExists(Doctor doctor, LocalDateTime startDateTime, LocalDateTime endDateTime, int capacity) {
        slotInventoryRepository.insertIgnoreSlotInventory(doctor.getId(), startDateTime, endDateTime, capacity);
    }
}
