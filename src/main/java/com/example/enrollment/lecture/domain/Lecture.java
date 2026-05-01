package com.example.enrollment.lecture.domain;

import com.example.enrollment.global.entity.BaseTimeEntity;
import com.example.enrollment.global.exception.BusinessException;
import com.example.enrollment.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "lecture")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LectureStatus status;

    @Column(nullable = false)
    private Long creatorId;

    @Builder
    public Lecture(String title, String description, Integer price,
                   Integer maxCapacity, LocalDate startDate, LocalDate endDate,
                   Long creatorId) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.maxCapacity = maxCapacity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = LectureStatus.DRAFT;
        this.creatorId = creatorId;
    }

    /**
     * 강의 상태 변경 (단방향: DRAFT → OPEN → CLOSED)
     */
    public void changeStatus(LectureStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    String.format("강의 상태를 %s에서 %s로 변경할 수 없습니다.", this.status, newStatus));
        }
        this.status = newStatus;
    }

    /**
     * 강의가 수강 신청 가능한 상태인지 확인
     */
    public boolean isOpen() {
        return this.status == LectureStatus.OPEN;
    }

    /**
     * 강의 소유자 확인
     */
    public boolean isOwnedBy(Long userId) {
        return this.creatorId.equals(userId);
    }
}
