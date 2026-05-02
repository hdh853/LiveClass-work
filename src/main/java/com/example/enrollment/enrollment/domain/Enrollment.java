package com.example.enrollment.enrollment.domain;

import com.example.enrollment.global.entity.BaseTimeEntity;
import com.example.enrollment.global.exception.BusinessException;
import com.example.enrollment.global.exception.ErrorCode;
import com.example.enrollment.lecture.domain.Lecture;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id", nullable = false)
    private Lecture lecture;

    @Column(nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    private Integer waitlistOrder;

    private LocalDateTime enrolledAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime cancelledAt;

    @Builder
    public Enrollment(Lecture lecture, Long studentId, EnrollmentStatus status, Integer waitlistOrder) {
        this.lecture = lecture;
        this.studentId = studentId;
        this.status = status;
        this.waitlistOrder = waitlistOrder;
        this.enrolledAt = LocalDateTime.now();
    }

    /**
     * 결제 확정 (PENDING → CONFIRMED)
     */
    public void confirm() {
        if (this.status != EnrollmentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ENROLLMENT_STATUS,
                    "결제 확정은 PENDING 상태에서만 가능합니다. 현재 상태: " + this.status);
        }
        this.status = EnrollmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    /**
     * 수강 취소
     * - PENDING, WAITLISTED: 즉시 취소
     * - CONFIRMED: 취소 가능 기간 확인 필요
     */
    public void cancel(int allowedCancelDays) {
        if (!this.status.isCancellable()) {
            throw new BusinessException(ErrorCode.INVALID_ENROLLMENT_STATUS,
                    "취소할 수 없는 상태입니다. 현재 상태: " + this.status);
        }

        if (this.status == EnrollmentStatus.CONFIRMED) {
            if (this.confirmedAt.plusDays(allowedCancelDays).isBefore(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.CANCEL_PERIOD_EXPIRED,
                        String.format("결제 확정 후 %d일이 지나 취소할 수 없습니다.", allowedCancelDays));
            }
        }

        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * 대기열에서 PENDING으로 승격
     */
    public void promoteFromWaitlist() {
        if (this.status != EnrollmentStatus.WAITLISTED) {
            throw new BusinessException(ErrorCode.INVALID_ENROLLMENT_STATUS,
                    "대기열 상태에서만 승격 가능합니다.");
        }
        this.status = EnrollmentStatus.PENDING;
        this.waitlistOrder = null;
        this.enrolledAt = LocalDateTime.now();
    }

    /**
     * 본인 확인
     */
    public boolean isOwnedBy(Long userId) {
        return this.studentId.equals(userId);
    }
}
