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
public class LectureResponse {

    private Long id;
    private String title;
    private String description;
    private Integer price;
    private Integer maxCapacity;
    private LocalDate startDate;
    private LocalDate endDate;
    private LectureStatus status;
    private Long creatorId;
    private LocalDateTime createdAt;

    public static LectureResponse from(Lecture lecture) {
        return LectureResponse.builder()
                .id(lecture.getId())
                .title(lecture.getTitle())
                .description(lecture.getDescription())
                .price(lecture.getPrice())
                .maxCapacity(lecture.getMaxCapacity())
                .startDate(lecture.getStartDate())
                .endDate(lecture.getEndDate())
                .status(lecture.getStatus())
                .creatorId(lecture.getCreatorId())
                .createdAt(lecture.getCreatedAt())
                .build();
    }
}
