package com.example.enrollment.enrollment.dto;

import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EnrollmentResponse {

    private Long id;
    private Long lectureId;
    private String lectureTitle;
    private Long studentId;
    private EnrollmentStatus status;
    private Integer waitlistOrder;
    private LocalDateTime enrolledAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;

    public static EnrollmentResponse from(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .lectureId(enrollment.getLecture().getId())
                .lectureTitle(enrollment.getLecture().getTitle())
                .studentId(enrollment.getStudentId())
                .status(enrollment.getStatus())
                .waitlistOrder(enrollment.getWaitlistOrder())
                .enrolledAt(enrollment.getEnrolledAt())
                .confirmedAt(enrollment.getConfirmedAt())
                .cancelledAt(enrollment.getCancelledAt())
                .createdAt(enrollment.getCreatedAt())
                .build();
    }
}
