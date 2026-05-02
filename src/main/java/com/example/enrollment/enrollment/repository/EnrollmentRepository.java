package com.example.enrollment.enrollment.repository;

import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * 활성 신청 인원 카운트 (PENDING + CONFIRMED)
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.lecture.id = :lectureId " +
            "AND e.status IN ('PENDING', 'CONFIRMED')")
    int countActiveEnrollments(@Param("lectureId") Long lectureId);

    /**
     * 중복 신청 확인 (CANCELLED 제외한 활성 신청이 있는지)
     */
    @Query("SELECT COUNT(e) > 0 FROM Enrollment e " +
            "WHERE e.lecture.id = :lectureId AND e.studentId = :studentId " +
            "AND e.status <> 'CANCELLED'")
    boolean existsActiveEnrollment(@Param("lectureId") Long lectureId, @Param("studentId") Long studentId);

    /**
     * 대기열에서 가장 오래된 항목 조회 (승격 대상)
     */
    Optional<Enrollment> findFirstByLectureIdAndStatusOrderByWaitlistOrderAsc(Long lectureId, EnrollmentStatus status);

    /**
     * 강의별 대기열 최대 순서 조회
     */
    @Query("SELECT COALESCE(MAX(e.waitlistOrder), 0) FROM Enrollment e " +
            "WHERE e.lecture.id = :lectureId AND e.status = 'WAITLISTED'")
    int findMaxWaitlistOrder(@Param("lectureId") Long lectureId);

    /**
     * 내 수강 신청 목록 조회 (페이지네이션)
     */
    Page<Enrollment> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    /**
     * 강의별 수강생 목록 조회 (PENDING + CONFIRMED만)
     */
    @Query("SELECT e FROM Enrollment e WHERE e.lecture.id = :lectureId " +
            "AND e.status IN ('PENDING', 'CONFIRMED') ORDER BY e.enrolledAt ASC")
    Page<Enrollment> findEnrolledStudents(@Param("lectureId") Long lectureId, Pageable pageable);

    /**
     * 강의별 대기열 목록 조회
     */
    List<Enrollment> findByLectureIdAndStatusOrderByWaitlistOrderAsc(Long lectureId, EnrollmentStatus status);
}
