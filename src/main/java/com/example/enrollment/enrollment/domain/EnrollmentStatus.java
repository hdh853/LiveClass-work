package com.example.enrollment.enrollment.domain;

public enum EnrollmentStatus {

    PENDING,        // 신청 완료, 결제 대기
    CONFIRMED,      // 결제 완료, 수강 확정
    CANCELLED,      // 취소됨
    WAITLISTED;     // 대기열 등록

    /**
     * 취소 가능한 상태인지 확인
     */
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED || this == WAITLISTED;
    }

    /**
     * 활성 상태인지 확인 (정원 카운트 대상)
     */
    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }
}
