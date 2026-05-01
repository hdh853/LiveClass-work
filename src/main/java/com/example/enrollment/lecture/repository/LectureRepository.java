package com.example.enrollment.lecture.repository;

import com.example.enrollment.lecture.domain.Lecture;
import com.example.enrollment.lecture.domain.LectureStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    /**
     * 상태별 강의 목록 조회 (페이지네이션)
     */
    Page<Lecture> findByStatus(LectureStatus status, Pageable pageable);

    /**
     * 전체 강의 목록 조회 (페이지네이션)
     */
    Page<Lecture> findAll(Pageable pageable);

    /**
     * 비관적 락을 사용한 강의 조회 (수강 신청 시 동시성 제어)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lecture l WHERE l.id = :id")
    Optional<Lecture> findByIdWithLock(@Param("id") Long id);

    /**
     * 크리에이터의 강의 목록 조회
     */
    Page<Lecture> findByCreatorId(Long creatorId, Pageable pageable);
}
