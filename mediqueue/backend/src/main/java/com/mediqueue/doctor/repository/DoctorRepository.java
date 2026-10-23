package com.mediqueue.doctor.repository;

import com.mediqueue.doctor.entity.Doctor;
import com.mediqueue.doctor.enums.DoctorVerificationStatus;
import com.mediqueue.user.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("SELECT d FROM Doctor d JOIN FETCH d.user JOIN FETCH d.department WHERE d.user.id = :userId")
    Optional<Doctor> findByUserId(@Param("userId") Long userId);

    @Query("SELECT d FROM Doctor d JOIN FETCH d.user JOIN FETCH d.department WHERE LOWER(d.user.email) = LOWER(:email)")
    Optional<Doctor> findByUserEmail(@Param("email") String email);

    boolean existsByUserId(Long userId);

    @Query(value = "SELECT d FROM Doctor d JOIN FETCH d.user u JOIN FETCH d.department dept " +
                   "WHERE d.verificationStatus = 'VERIFIED' " +
                   "AND u.accountStatus = 'ACTIVE' " +
                   "AND dept.active = true " +
                   "AND (:departmentId IS NULL OR dept.id = :departmentId) " +
                   "AND (:specialty IS NULL OR LOWER(d.specialty) LIKE LOWER(CONCAT('%', :specialty, '%')))",
           countQuery = "SELECT count(d) FROM Doctor d JOIN d.user u JOIN d.department dept " +
                        "WHERE d.verificationStatus = 'VERIFIED' " +
                        "AND u.accountStatus = 'ACTIVE' " +
                        "AND dept.active = true " +
                        "AND (:departmentId IS NULL OR dept.id = :departmentId) " +
                        "AND (:specialty IS NULL OR LOWER(d.specialty) LIKE LOWER(CONCAT('%', :specialty, '%')))")
    Page<Doctor> findPublicDoctors(
            @Param("departmentId") Long departmentId,
            @Param("specialty") String specialty,
            Pageable pageable
    );

    @Query("SELECT d FROM Doctor d JOIN FETCH d.user u JOIN FETCH d.department dept " +
           "WHERE d.id = :id " +
           "AND d.verificationStatus = 'VERIFIED' " +
           "AND u.accountStatus = 'ACTIVE' " +
           "AND dept.active = true")
    Optional<Doctor> findPublicDoctorById(@Param("id") Long id);

    @Query(value = "SELECT d FROM Doctor d JOIN FETCH d.user u JOIN FETCH d.department dept " +
                   "WHERE (:verificationStatus IS NULL OR d.verificationStatus = :verificationStatus) " +
                   "AND (:departmentId IS NULL OR dept.id = :departmentId)",
           countQuery = "SELECT count(d) FROM Doctor d " +
                        "WHERE (:verificationStatus IS NULL OR d.verificationStatus = :verificationStatus) " +
                        "AND (:departmentId IS NULL OR d.department.id = :departmentId)")
    Page<Doctor> findAdminDoctors(
            @Param("verificationStatus") DoctorVerificationStatus verificationStatus,
            @Param("departmentId") Long departmentId,
            Pageable pageable
    );

    java.util.List<Doctor> findByDepartmentId(Long departmentId);
}
