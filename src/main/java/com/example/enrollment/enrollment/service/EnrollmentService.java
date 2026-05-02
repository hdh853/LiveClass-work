package com.example.enrollment.enrollment.service;

import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.enrollment.dto.EnrollmentCreateRequest;
import com.example.enrollment.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.global.exception.BusinessException;
import com.example.enrollment.global.exception.ErrorCode;
import com.example.enrollment.global.resolver.UserInfo;
import com.example.enrollment.lecture.domain.Lecture;
import com.example.enrollment.lecture.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;

    @Value("${enrollment.cancel.allowed-days:7}")
    private int allowedCancelDays;

    /**
     * 수강 신청
     * 1. 강의 상태 확인 (OPEN만 가능)
     * 2. 중복 신청 확인
     * 3. 비관적 락으로 강의 조회
     * 4. 정원 확인 → PENDING 또는 WAITLISTED
     */
    @Transactional
    public EnrollmentResponse enroll(UserInfo userInfo, EnrollmentCreateRequest request) {
        if (!userInfo.isStudent()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS, "수강 신청은 수강생만 가능합니다.");
        }

        // 1. 강의 존재 및 상태 확인
        Lecture lecture = lectureRepository.findById(request.getLectureId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        if (!lecture.isOpen()) {
            throw new BusinessException(ErrorCode.LECTURE_NOT_OPEN);
        }

        // 2. 중복 신청 확인
        if (enrollmentRepository.existsActiveEnrollment(lecture.getId(), userInfo.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ENROLLMENT);
        }

        // 3. 비관적 락으로 강의 조회 (동시성 제어)
        Lecture lockedLecture = lectureRepository.findByIdWithLock(lecture.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        // 4. 정원 확인
        int activeCount = enrollmentRepository.countActiveEnrollments(lockedLecture.getId());

        Enrollment enrollment;
        if (activeCount < lockedLecture.getMaxCapacity()) {
            // 정원 여유 → PENDING
            enrollment = Enrollment.builder()
                    .lecture(lockedLecture)
                    .studentId(userInfo.getId())
                    .status(EnrollmentStatus.PENDING)
                    .build();
            log.info("수강 신청 완료: lectureId={}, studentId={}, status=PENDING", lockedLecture.getId(), userInfo.getId());
        } else {
            // 정원 초과 → WAITLISTED
            int maxOrder = enrollmentRepository.findMaxWaitlistOrder(lockedLecture.getId());
            enrollment = Enrollment.builder()
                    .lecture(lockedLecture)
                    .studentId(userInfo.getId())
                    .status(EnrollmentStatus.WAITLISTED)
                    .waitlistOrder(maxOrder + 1)
                    .build();
            log.info("대기열 등록: lectureId={}, studentId={}, waitlistOrder={}", lockedLecture.getId(), userInfo.getId(), maxOrder + 1);
        }

        Enrollment saved = enrollmentRepository.save(enrollment);
        return EnrollmentResponse.from(saved);
    }

    /**
     * 결제 확정 (PENDING → CONFIRMED)
     */
    @Transactional
    public EnrollmentResponse confirmPayment(Long enrollmentId, UserInfo userInfo) {
        Enrollment enrollment = findEnrollmentById(enrollmentId);

        if (!enrollment.isOwnedBy(userInfo.getId())) {
            throw new BusinessException(ErrorCode.NOT_ENROLLMENT_OWNER);
        }

        enrollment.confirm();
        log.info("결제 확정: enrollmentId={}, studentId={}", enrollmentId, userInfo.getId());
        return EnrollmentResponse.from(enrollment);
    }

    /**
     * 수강 취소
     * - PENDING / WAITLISTED: 즉시 취소
     * - CONFIRMED: 7일 이내만 취소 가능
     * - 취소 후 대기열 1순위 → PENDING 승격
     */
    @Transactional
    public EnrollmentResponse cancelEnrollment(Long enrollmentId, UserInfo userInfo) {
        Enrollment enrollment = findEnrollmentById(enrollmentId);

        if (!enrollment.isOwnedBy(userInfo.getId())) {
            throw new BusinessException(ErrorCode.NOT_ENROLLMENT_OWNER);
        }

        boolean wasActive = enrollment.getStatus().isActive();
        Long lectureId = enrollment.getLecture().getId();

        // 취소 처리 (CONFIRMED의 경우 기간 체크 포함)
        enrollment.cancel(allowedCancelDays);
        log.info("수강 취소: enrollmentId={}, studentId={}", enrollmentId, userInfo.getId());

        // 활성 상태(PENDING/CONFIRMED)에서 취소된 경우 대기열 승격
        if (wasActive) {
            promoteFromWaitlist(lectureId);
        }

        return EnrollmentResponse.from(enrollment);
    }

    /**
     * 내 수강 신청 목록 조회 (페이지네이션)
     */
    public Page<EnrollmentResponse> getMyEnrollments(UserInfo userInfo, Pageable pageable) {
        Page<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdOrderByCreatedAtDesc(userInfo.getId(), pageable);
        return enrollments.map(EnrollmentResponse::from);
    }

    /**
     * 강의별 수강생 목록 조회 (CREATOR 전용)
     */
    public Page<EnrollmentResponse> getEnrolledStudents(Long lectureId, UserInfo userInfo, Pageable pageable) {
        if (!userInfo.isCreator()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS, "크리에이터만 조회 가능합니다.");
        }

        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

        if (!lecture.isOwnedBy(userInfo.getId())) {
            throw new BusinessException(ErrorCode.NOT_LECTURE_OWNER);
        }

        Page<Enrollment> enrollments = enrollmentRepository.findEnrolledStudents(lectureId, pageable);
        return enrollments.map(EnrollmentResponse::from);
    }

    /**
     * 활성 신청 인원 카운트 (LectureService에서 사용)
     */
    public int getActiveEnrollmentCount(Long lectureId) {
        return enrollmentRepository.countActiveEnrollments(lectureId);
    }

    /**
     * 대기열에서 PENDING으로 승격
     */
    private void promoteFromWaitlist(Long lectureId) {
        Optional<Enrollment> waitlisted = enrollmentRepository
                .findFirstByLectureIdAndStatusOrderByWaitlistOrderAsc(lectureId, EnrollmentStatus.WAITLISTED);

        waitlisted.ifPresent(enrollment -> {
            enrollment.promoteFromWaitlist();
            log.info("대기열 승격: enrollmentId={}, studentId={}, lectureId={}",
                    enrollment.getId(), enrollment.getStudentId(), lectureId);
        });
    }

    private Enrollment findEnrollmentById(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}
