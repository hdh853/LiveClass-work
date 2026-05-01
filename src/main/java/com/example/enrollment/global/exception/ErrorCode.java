package com.example.enrollment.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "C002", "접근 권한이 없습니다."),
    MISSING_USER_INFO(HttpStatus.UNAUTHORIZED, "C003", "사용자 정보가 누락되었습니다."),

    // Lecture
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "강의를 찾을 수 없습니다."),
    LECTURE_NOT_OPEN(HttpStatus.BAD_REQUEST, "L002", "모집 중이 아닌 강의입니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "L003", "잘못된 상태 전환입니다."),
    NOT_LECTURE_OWNER(HttpStatus.FORBIDDEN, "L004", "강의 소유자만 가능합니다."),

    // Enrollment
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "수강 신청 내역을 찾을 수 없습니다."),
    DUPLICATE_ENROLLMENT(HttpStatus.CONFLICT, "E002", "이미 신청한 강의입니다."),
    INVALID_ENROLLMENT_STATUS(HttpStatus.BAD_REQUEST, "E003", "잘못된 신청 상태입니다."),
    CANCEL_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "E004", "취소 가능 기간이 만료되었습니다."),
    CAPACITY_FULL_WAITLISTED(HttpStatus.OK, "E005", "정원이 초과되어 대기열에 등록되었습니다."),
    NOT_ENROLLMENT_OWNER(HttpStatus.FORBIDDEN, "E006", "본인의 수강 신청만 처리 가능합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
