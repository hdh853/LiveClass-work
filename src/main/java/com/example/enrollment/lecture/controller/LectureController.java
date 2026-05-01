package com.example.enrollment.lecture.controller;

import com.example.enrollment.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.enrollment.service.EnrollmentService;
import com.example.enrollment.global.resolver.CurrentUser;
import com.example.enrollment.global.resolver.UserInfo;
import com.example.enrollment.lecture.dto.*;
import com.example.enrollment.lecture.service.LectureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final LectureService lectureService;
    private final EnrollmentService enrollmentService;

    /**
     * 강의 등록
     * POST /api/lectures
     */
    @PostMapping
    public ResponseEntity<LectureResponse> createLecture(
            @CurrentUser UserInfo userInfo,
            @Valid @RequestBody LectureCreateRequest request) {
        LectureResponse response = lectureService.createLecture(userInfo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 강의 목록 조회 (상태 필터 가능)
     * GET /api/lectures?status=OPEN&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<LectureResponse>> getLectures(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LectureResponse> response = lectureService.getLectures(status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 강의 상세 조회
     * GET /api/lectures/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<LectureDetailResponse> getLectureDetail(@PathVariable Long id) {
        LectureDetailResponse response = lectureService.getLectureDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 강의 상태 변경
     * PATCH /api/lectures/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<LectureResponse> updateLectureStatus(
            @PathVariable Long id,
            @CurrentUser UserInfo userInfo,
            @Valid @RequestBody LectureStatusUpdateRequest request) {
        LectureResponse response = lectureService.updateLectureStatus(id, userInfo, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 강의별 수강생 목록 조회 (크리에이터 전용)
     * GET /api/lectures/{id}/students?page=0&size=10
     */
    @GetMapping("/{id}/students")
    public ResponseEntity<Page<EnrollmentResponse>> getEnrolledStudents(
            @PathVariable Long id,
            @CurrentUser UserInfo userInfo,
            @PageableDefault(size = 10, sort = "enrolledAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<EnrollmentResponse> response = enrollmentService.getEnrolledStudents(id, userInfo, pageable);
        return ResponseEntity.ok(response);
    }
}
