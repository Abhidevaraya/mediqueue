package com.mediqueue.schedule.repository;

import com.mediqueue.schedule.entity.SlotInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SlotInventoryRepository extends JpaRepository<SlotInventory, Long> {

    Optional<SlotInventory> findByDoctorIdAndSlotStartTime(Long doctorId, LocalDateTime slotStartTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SlotInventory s WHERE s.doctor.id = :doctorId AND s.slotStartTime = :slotStartTime")
    Optional<SlotInventory> findByDoctorIdAndSlotStartTimeWithLock(
            @Param("doctorId") Long doctorId,
            @Param("slotStartTime") LocalDateTime slotStartTime
    );

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "INSERT IGNORE INTO slot_inventories (doctor_id, slot_start_time, slot_end_time, total_capacity, booked_count, version, created_at, updated_at) " +
            "VALUES (:doctorId, :slotStartTime, :slotEndTime, :totalCapacity, 0, 0, NOW(6), NOW(6))", nativeQuery = true)
    int insertIgnoreSlotInventory(
            @Param("doctorId") Long doctorId,
            @Param("slotStartTime") LocalDateTime slotStartTime,
            @Param("slotEndTime") LocalDateTime slotEndTime,
            @Param("totalCapacity") int totalCapacity
    );
}
