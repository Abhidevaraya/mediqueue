package com.mediqueue.patient.repository;

import com.mediqueue.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("SELECT p FROM Patient p JOIN FETCH p.user WHERE p.user.id = :userId")
    Optional<Patient> findByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Patient p JOIN FETCH p.user WHERE LOWER(p.user.email) = LOWER(:email)")
    Optional<Patient> findByUserEmail(@Param("email") String email);

    boolean existsByUserId(Long userId);
}
