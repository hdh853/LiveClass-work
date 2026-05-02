package com.example.enrollment.enrollment.controller;

import com.example.enrollment.enrollment.dto.EnrollmentCreateRequest;
import com.example.enrollment.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.enrollment.service.EnrollmentService;
import com.example.enrollment.global.resolver.CurrentUser;
import com.example.enrollment.global.resolver.UserInfo;
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
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * 수강 신청
     * POST /api/enrollments
     */
    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(
            @CurrentUser UserInfo userInfo,
            @Valid @RequestBody EnrollmentCreateRequest request) {
        EnrollmentResponse response = enrollmentService.enroll(userInfo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 수강 신청 목록 조회
     * GET /api/enrollments/me?page=0&size=10
     */
    @GetMapping("/me")
    public ResponseEntity<Page<EnrollmentResponse>> getMyEnrollments(
            @CurrentUser UserInfo userInfo,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<EnrollmentResponse> response = enrollmentService.getMyEnrollments(userInfo, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 확정
     * PATCH /api/enrollments/{id}/confirm
     */
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<EnrollmentResponse> confirmPayment(
            @PathVariable Long id,
            @CurrentUser UserInfo userInfo) {
        EnrollmentResponse response = enrollmentService.confirmPayment(id, userInfo);
        return ResponseEntity.ok(response);
    }

    /**
     * 수강 취소
     * PATCH /api/enrollments/{id}/cancel
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @PathVariable Long id,
            @CurrentUser UserInfo userInfo) {
        EnrollmentResponse response = enrollmentService.cancelEnrollment(id, userInfo);
        return ResponseEntity.ok(response);
    }
}
