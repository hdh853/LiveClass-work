package com.example.enrollment.lecture.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LectureCreateRequest {

    @NotBlank(message = "강의 제목은 필수입니다.")
    @Size(max = 200, message = "강의 제목은 200자 이하여야 합니다.")
    private String title;

    private String description;

    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Integer price;

    @NotNull(message = "최대 정원은 필수입니다.")
    @Min(value = 1, message = "최대 정원은 1명 이상이어야 합니다.")
    private Integer maxCapacity;

    @NotNull(message = "시작일은 필수입니다.")
    @FutureOrPresent(message = "시작일은 오늘 이후여야 합니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    @Future(message = "종료일은 미래여야 합니다.")
    private LocalDate endDate;
}
