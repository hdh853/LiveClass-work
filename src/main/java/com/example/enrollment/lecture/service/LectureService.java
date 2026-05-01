package com.example.enrollment.lecture.service;

import com.example.enrollment.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.global.exception.BusinessException;
import com.example.enrollment.global.exception.ErrorCode;
import com.example.enrollment.global.resolver.UserInfo;
import com.example.enrollment.lecture.domain.Lecture;
import com.example.enrollment.lecture.domain.LectureStatus;
import com.example.enrollment.lecture.dto.*;
import com.example.enrollment.lecture.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureService {

    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * 강의 등록 (CREATOR 전용)
     */
    @Transactional
    public LectureResponse createLecture(UserInfo userInfo, LectureCreateRequest request) {
        if (!userInfo.isCreator()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS, "강의 등록은 크리에이터만 가능합니다.");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "종료일은 시작일 이후여야 합니다.");
        }

        Lecture lecture = Lecture.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .maxCapacity(request.getMaxCapacity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .creatorId(userInfo.getId())
                .build();

        Lecture saved = lectureRepository.save(lecture);
        log.info("강의 등록 완료: id={}, title={}", saved.getId(), saved.getTitle());
        return LectureResponse.from(saved);
    }

    /**
     * 강의 상태 변경 (DRAFT → OPEN → CLOSED)
     */
    @Transactional
    public LectureResponse updateLectureStatus(Long lectureId, UserInfo userInfo, LectureStatusUpdateRequest request) {
        Lecture lecture = findLectureById(lectureId);

        if (!lecture.isOwnedBy(userInfo.getId())) {
            throw new BusinessException(ErrorCode.NOT_LECTURE_OWNER);
        }

        LectureStatus newStatus;
        try {
            newStatus = LectureStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "유효하지 않은 상태입니다: " + request.getStatus());
        }

        lecture.changeStatus(newStatus);
        log.info("강의 상태 변경: id={}, newStatus={}", lectureId, newStatus);
        return LectureResponse.from(lecture);
    }

    /**
     * 강의 목록 조회 (상태 필터 가능)
     */
    public Page<LectureResponse> getLectures(String status, Pageable pageable) {
        Page<Lecture> lectures;
        if (status != null && !status.isBlank()) {
            try {
                LectureStatus lectureStatus = LectureStatus.valueOf(status.toUpperCase());
                lectures = lectureRepository.findByStatus(lectureStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 상태 필터입니다: " + status);
            }
        } else {
            lectures = lectureRepository.findAll(pageable);
        }
        return lectures.map(LectureResponse::from);
    }

    /**
     * 강의 상세 조회 (현재 신청 인원 포함)
     */
    public LectureDetailResponse getLectureDetail(Long lectureId) {
        Lecture lecture = findLectureById(lectureId);
        int activeCount = enrollmentRepository.countActiveEnrollments(lectureId);
        return LectureDetailResponse.of(lecture, activeCount);
    }

    /**
     * 강의 엔티티 조회 (내부 사용)
     */
    public Lecture findLectureById(Long lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));
    }
}
