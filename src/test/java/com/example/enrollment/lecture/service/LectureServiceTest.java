package com.example.enrollment.lecture.service;

import com.example.enrollment.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.global.exception.BusinessException;
import com.example.enrollment.global.resolver.UserInfo;
import com.example.enrollment.lecture.domain.Lecture;
import com.example.enrollment.lecture.domain.LectureStatus;
import com.example.enrollment.lecture.dto.LectureCreateRequest;
import com.example.enrollment.lecture.dto.LectureDetailResponse;
import com.example.enrollment.lecture.dto.LectureResponse;
import com.example.enrollment.lecture.dto.LectureStatusUpdateRequest;
import com.example.enrollment.lecture.repository.LectureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LectureServiceTest {

    @InjectMocks
    private LectureService lectureService;

    @Mock
    private LectureRepository lectureRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    private UserInfo creatorInfo;
    private UserInfo studentInfo;

    @BeforeEach
    void setUp() {
        creatorInfo = new UserInfo(1L, UserInfo.UserRole.CREATOR);
        studentInfo = new UserInfo(10L, UserInfo.UserRole.STUDENT);
    }

    @Nested
    @DisplayName("강의 등록")
    class CreateLecture {

        @Test
        @DisplayName("크리에이터가 강의를 등록하면 DRAFT 상태로 생성된다")
        void success() {
            // given
            LectureCreateRequest request = new LectureCreateRequest(
                    "Spring Boot 입문", "설명", 50000, 30,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

            given(lectureRepository.save(any(Lecture.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            LectureResponse response = lectureService.createLecture(creatorInfo, request);

            // then
            assertThat(response.getTitle()).isEqualTo("Spring Boot 입문");
            assertThat(response.getStatus()).isEqualTo(LectureStatus.DRAFT);
        }

        @Test
        @DisplayName("수강생이 강의를 등록하면 예외가 발생한다")
        void failWhenStudentCreates() {
            LectureCreateRequest request = new LectureCreateRequest(
                    "강의", "설명", 50000, 30,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

            assertThatThrownBy(() -> lectureService.createLecture(studentInfo, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("종료일이 시작일보다 이전이면 예외가 발생한다")
        void failWhenEndDateBeforeStartDate() {
            LectureCreateRequest request = new LectureCreateRequest(
                    "강의", "설명", 50000, 30,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 1));

            assertThatThrownBy(() -> lectureService.createLecture(creatorInfo, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("강의 상태 변경")
    class UpdateStatus {

        @Test
        @DisplayName("DRAFT → OPEN 상태 변경이 성공한다")
        void draftToOpen() {
            Lecture lecture = Lecture.builder()
                    .title("강의").description("설명").price(50000).maxCapacity(30)
                    .startDate(LocalDate.of(2026, 6, 1)).endDate(LocalDate.of(2026, 6, 30))
                    .creatorId(1L).build();

            given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));

            LectureResponse response = lectureService.updateLectureStatus(
                    1L, creatorInfo, new LectureStatusUpdateRequest("OPEN"));

            assertThat(response.getStatus()).isEqualTo(LectureStatus.OPEN);
        }

        @Test
        @DisplayName("DRAFT → CLOSED 상태 변경은 실패한다")
        void draftToClosedFails() {
            Lecture lecture = Lecture.builder()
                    .title("강의").description("설명").price(50000).maxCapacity(30)
                    .startDate(LocalDate.of(2026, 6, 1)).endDate(LocalDate.of(2026, 6, 30))
                    .creatorId(1L).build();

            given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));

            assertThatThrownBy(() -> lectureService.updateLectureStatus(
                    1L, creatorInfo, new LectureStatusUpdateRequest("CLOSED")))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("소유자가 아닌 크리에이터가 상태를 변경하면 예외가 발생한다")
        void failWhenNotOwner() {
            Lecture lecture = Lecture.builder()
                    .title("강의").description("설명").price(50000).maxCapacity(30)
                    .startDate(LocalDate.of(2026, 6, 1)).endDate(LocalDate.of(2026, 6, 30))
                    .creatorId(999L).build();

            given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));

            assertThatThrownBy(() -> lectureService.updateLectureStatus(
                    1L, creatorInfo, new LectureStatusUpdateRequest("OPEN")))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("강의 조회")
    class GetLectures {

        @Test
        @DisplayName("강의 목록을 상태 필터로 조회한다")
        void getLecturesWithFilter() {
            Lecture lecture = Lecture.builder()
                    .title("강의").description("설명").price(50000).maxCapacity(30)
                    .startDate(LocalDate.of(2026, 6, 1)).endDate(LocalDate.of(2026, 6, 30))
                    .creatorId(1L).build();

            Page<Lecture> page = new PageImpl<>(List.of(lecture));
            given(lectureRepository.findByStatus(eq(LectureStatus.DRAFT), any(PageRequest.class))).willReturn(page);

            Page<LectureResponse> result = lectureService.getLectures("DRAFT", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("강의 상세 조회 시 현재 신청 인원이 포함된다")
        void getLectureDetail() {
            Lecture lecture = Lecture.builder()
                    .title("강의").description("설명").price(50000).maxCapacity(30)
                    .startDate(LocalDate.of(2026, 6, 1)).endDate(LocalDate.of(2026, 6, 30))
                    .creatorId(1L).build();

            given(lectureRepository.findById(1L)).willReturn(Optional.of(lecture));
            given(enrollmentRepository.countActiveEnrollments(1L)).willReturn(5);

            LectureDetailResponse response = lectureService.getLectureDetail(1L);

            assertThat(response.getCurrentEnrollmentCount()).isEqualTo(5);
            assertThat(response.getRemainingCapacity()).isEqualTo(25);
        }
    }
}
