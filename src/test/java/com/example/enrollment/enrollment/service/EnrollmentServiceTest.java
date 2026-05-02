package com.example.enrollment.enrollment.service;

import com.example.enrollment.enrollment.domain.Enrollment;
import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.enrollment.dto.EnrollmentCreateRequest;
import com.example.enrollment.enrollment.dto.EnrollmentResponse;
import com.example.enrollment.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.global.exception.BusinessException;
import com.example.enrollment.global.resolver.UserInfo;
import com.example.enrollment.lecture.domain.Lecture;
import com.example.enrollment.lecture.domain.LectureStatus;
import com.example.enrollment.lecture.repository.LectureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LectureRepository lectureRepository;

    private UserInfo studentInfo;
    private UserInfo creatorInfo;
    private Lecture openLecture;

    @BeforeEach
    void setUp() {
        studentInfo = new UserInfo(10L, UserInfo.UserRole.STUDENT);
        creatorInfo = new UserInfo(1L, UserInfo.UserRole.CREATOR);

        openLecture = Lecture.builder()
                .title("Spring Boot 입문")
                .description("설명")
                .price(50000)
                .maxCapacity(3)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .creatorId(1L)
                .build();
        // DRAFT → OPEN
        openLecture.changeStatus(LectureStatus.OPEN);

        ReflectionTestUtils.setField(openLecture, "id", 1L);
        ReflectionTestUtils.setField(enrollmentService, "allowedCancelDays", 7);
    }

    @Nested
    @DisplayName("수강 신청")
    class Enroll {

        @Test
        @DisplayName("정상 수강 신청 → PENDING")
        void success() {
            given(lectureRepository.findById(1L)).willReturn(Optional.of(openLecture));
            given(enrollmentRepository.existsActiveEnrollment(1L, 10L)).willReturn(false);
            given(lectureRepository.findByIdWithLock(1L)).willReturn(Optional.of(openLecture));
            given(enrollmentRepository.countActiveEnrollments(1L)).willReturn(0);
            given(enrollmentRepository.save(any(Enrollment.class))).willAnswer(invocation -> {
                Enrollment e = invocation.getArgument(0);
                ReflectionTestUtils.setField(e, "id", 1L);
                return e;
            });

            EnrollmentResponse response = enrollmentService.enroll(studentInfo, new EnrollmentCreateRequest(1L));

            assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        }

        @Test
        @DisplayName("정원 초과 시 → WAITLISTED")
        void waitlisted() {
            given(lectureRepository.findById(1L)).willReturn(Optional.of(openLecture));
            given(enrollmentRepository.existsActiveEnrollment(1L, 10L)).willReturn(false);
            given(lectureRepository.findByIdWithLock(1L)).willReturn(Optional.of(openLecture));
            given(enrollmentRepository.countActiveEnrollments(1L)).willReturn(3); // maxCapacity = 3
            given(enrollmentRepository.findMaxWaitlistOrder(1L)).willReturn(0);
            given(enrollmentRepository.save(any(Enrollment.class))).willAnswer(invocation -> {
                Enrollment e = invocation.getArgument(0);
                ReflectionTestUtils.setField(e, "id", 1L);
                return e;
            });

            EnrollmentResponse response = enrollmentService.enroll(studentInfo, new EnrollmentCreateRequest(1L));

            assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
            assertThat(response.getWaitlistOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("중복 신청 시 예외 발생")
        void duplicateEnrollment() {
            given(lectureRepository.findById(1L)).willReturn(Optional.of(openLecture));
            given(enrollmentRepository.existsActiveEnrollment(1L, 10L)).willReturn(true);

            assertThatThrownBy(() -> enrollmentService.enroll(studentInfo, new EnrollmentCreateRequest(1L)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("DRAFT 상태 강의에 신청하면 예외 발생")
        void enrollOnDraftLecture() {
            Lecture draftLecture = Lecture.builder()
                    .title("강의").description("설명").price(50000).maxCapacity(30)
                    .startDate(LocalDate.of(2026, 6, 1)).endDate(LocalDate.of(2026, 6, 30))
                    .creatorId(1L).build();
            ReflectionTestUtils.setField(draftLecture, "id", 2L);

            given(lectureRepository.findById(2L)).willReturn(Optional.of(draftLecture));

            assertThatThrownBy(() -> enrollmentService.enroll(studentInfo, new EnrollmentCreateRequest(2L)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("크리에이터가 수강 신청하면 예외 발생")
        void creatorCannotEnroll() {
            assertThatThrownBy(() -> enrollmentService.enroll(creatorInfo, new EnrollmentCreateRequest(1L)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("결제 확정")
    class ConfirmPayment {

        @Test
        @DisplayName("PENDING → CONFIRMED 성공")
        void success() {
            Enrollment enrollment = Enrollment.builder()
                    .lecture(openLecture)
                    .studentId(10L)
                    .status(EnrollmentStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(enrollment, "id", 1L);

            given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

            EnrollmentResponse response = enrollmentService.confirmPayment(1L, studentInfo);

            assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
            assertThat(response.getConfirmedAt()).isNotNull();
        }

        @Test
        @DisplayName("CONFIRMED 상태에서 다시 확정하면 예외 발생")
        void alreadyConfirmed() {
            Enrollment enrollment = Enrollment.builder()
                    .lecture(openLecture)
                    .studentId(10L)
                    .status(EnrollmentStatus.PENDING)
                    .build();
            enrollment.confirm(); // CONFIRMED로 변경
            ReflectionTestUtils.setField(enrollment, "id", 1L);

            given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

            assertThatThrownBy(() -> enrollmentService.confirmPayment(1L, studentInfo))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("본인이 아닌 신청을 확정하면 예외 발생")
        void notOwner() {
            Enrollment enrollment = Enrollment.builder()
                    .lecture(openLecture)
                    .studentId(99L) // 다른 사용자
                    .status(EnrollmentStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(enrollment, "id", 1L);

            given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

            assertThatThrownBy(() -> enrollmentService.confirmPayment(1L, studentInfo))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("수강 취소")
    class CancelEnrollment {

        @Test
        @DisplayName("PENDING 상태에서 즉시 취소 성공")
        void cancelPending() {
            Enrollment enrollment = Enrollment.builder()
                    .lecture(openLecture)
                    .studentId(10L)
                    .status(EnrollmentStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(enrollment, "id", 1L);

            given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

            EnrollmentResponse response = enrollmentService.cancelEnrollment(1L, studentInfo);

            assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        }

        @Test
        @DisplayName("CONFIRMED 후 7일 이내 취소 성공")
        void cancelConfirmedWithinPeriod() {
            Enrollment enrollment = Enrollment.builder()
                    .lecture(openLecture)
                    .studentId(10L)
                    .status(EnrollmentStatus.PENDING)
                    .build();
            enrollment.confirm();
            ReflectionTestUtils.setField(enrollment, "id", 1L);
            // confirmedAt이 방금 설정됨 → 7일 이내

            given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

            EnrollmentResponse response = enrollmentService.cancelEnrollment(1L, studentInfo);

            assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        }

        @Test
        @DisplayName("CONFIRMED 후 7일 이후 취소 시 예외 발생")
        void cancelConfirmedAfterPeriod() {
            Enrollment enrollment = Enrollment.builder()
                    .lecture(openLecture)
                    .studentId(10L)
                    .status(EnrollmentStatus.PENDING)
                    .build();
            enrollment.confirm();
            ReflectionTestUtils.setField(enrollment, "id", 1L);
            // confirmedAt을 8일 전으로 변경
            ReflectionTestUtils.setField(enrollment, "confirmedAt", LocalDateTime.now().minusDays(8));

            given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

            assertThatThrownBy(() -> enrollmentService.cancelEnrollment(1L, studentInfo))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("취소 시 대기열 1순위가 PENDING으로 승격된다")
        void promoteFromWaitlistOnCancel() {
            Enrollment enrollment = Enrollment.builder()
                    .lecture(openLecture)
                    .studentId(10L)
                    .status(EnrollmentStatus.PENDING)
                    .build();
            ReflectionTestUtils.setField(enrollment, "id", 1L);

            Enrollment waitlisted = Enrollment.builder()
                    .lecture(openLecture)
                    .studentId(20L)
                    .status(EnrollmentStatus.WAITLISTED)
                    .waitlistOrder(1)
                    .build();
            ReflectionTestUtils.setField(waitlisted, "id", 2L);

            given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));
            given(enrollmentRepository.findFirstByLectureIdAndStatusOrderByWaitlistOrderAsc(
                    eq(1L), eq(EnrollmentStatus.WAITLISTED)))
                    .willReturn(Optional.of(waitlisted));

            enrollmentService.cancelEnrollment(1L, studentInfo);

            assertThat(waitlisted.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
            assertThat(waitlisted.getWaitlistOrder()).isNull();
        }

        @Test
        @DisplayName("WAITLISTED 상태에서 취소 성공")
        void cancelWaitlisted() {
            Enrollment enrollment = Enrollment.builder()
                    .lecture(openLecture)
                    .studentId(10L)
                    .status(EnrollmentStatus.WAITLISTED)
                    .waitlistOrder(1)
                    .build();
            ReflectionTestUtils.setField(enrollment, "id", 1L);

            given(enrollmentRepository.findById(1L)).willReturn(Optional.of(enrollment));

            EnrollmentResponse response = enrollmentService.cancelEnrollment(1L, studentInfo);

            assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        }
    }
}
