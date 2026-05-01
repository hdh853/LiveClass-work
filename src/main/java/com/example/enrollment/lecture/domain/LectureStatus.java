package com.example.enrollment.lecture.domain;

public enum LectureStatus {

    DRAFT,      // 초안 (신청 불가)
    OPEN,       // 모집 중 (신청 가능)
    CLOSED;     // 모집 마감 (신청 불가)

    /**
     * 상태 전환 유효성 검사
     * DRAFT → OPEN → CLOSED (단방향)
     */
    public boolean canTransitionTo(LectureStatus target) {
        return switch (this) {
            case DRAFT -> target == OPEN;
            case OPEN -> target == CLOSED;
            case CLOSED -> false;
        };
    }
}
