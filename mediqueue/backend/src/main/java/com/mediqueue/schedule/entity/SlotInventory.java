package com.mediqueue.schedule.entity;

import com.mediqueue.common.BaseEntity;
import com.mediqueue.doctor.entity.Doctor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "slot_inventories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_slot_inv_doctor_start", columnNames = {"doctor_id", "slot_start_time"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotInventory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "slot_start_time", nullable = false)
    private LocalDateTime slotStartTime;

    @Column(name = "slot_end_time", nullable = false)
    private LocalDateTime slotEndTime;

    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @Column(name = "booked_count", nullable = false)
    @Builder.Default
    private Integer bookedCount = 0;
}
