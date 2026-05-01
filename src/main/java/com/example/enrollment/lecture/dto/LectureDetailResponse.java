package com.example.enrollment.lecture.dto;

import com.example.enrollment.lecture.domain.Lecture;
import com.example.enrollment.lecture.domain.LectureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class LectureDetailResponse {

    private Long id;
    private String title;
    private String description;
    private Integer price;
    private Integer maxCapacity;
    private int currentEnrollmentCount;
    private int remainingCapacity;
    private LocalDate startDate;
    private LocalDate endDate;
    private LectureStatus status;
    private Long creatorId;
    private LocalDateTime createdAt;

    public static LectureDetailResponse of(Lecture lecture, int currentEnrollmentCount) {
        return LectureDetailResponse.builder()
                .id(lecture.getId())
                .title(lecture.getTitle())
                .description(lecture.getDescription())
                .price(lecture.getPrice())
                .maxCapacity(lecture.getMaxCapacity())
                .currentEnrollmentCount(currentEnrollmentCount)
                .remainingCapacity(lecture.getMaxCapacity() - currentEnrollmentCount)
                .startDate(lecture.getStartDate())
                .endDate(lecture.getEndDate())
                .status(lecture.getStatus())
                .creatorId(lecture.getCreatorId())
                .createdAt(lecture.getCreatedAt())
                .build();
    }
}
