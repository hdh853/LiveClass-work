package com.example.enrollment.lecture.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LectureStatusUpdateRequest {

    @NotNull(message = "변경할 상태는 필수입니다.")
    private String status;
}
