package com.example.enrollment.enrollment.service;

import com.example.enrollment.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.enrollment.dto.EnrollmentCreateRequest;
import com.example.enrollment.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.global.resolver.UserInfo;
import com.example.enrollment.lecture.domain.Lecture;
import com.example.enrollment.lecture.domain.LectureStatus;
import com.example.enrollment.lecture.repository.LectureRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class EnrollmentServiceConcurrencyTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @AfterEach
    void tearDown() {
        enrollmentRepository.deleteAllInBatch();
        lectureRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("정원 3명인 강의에 10명이 동시에 수강 신청할 경우 3명만 PENDING, 7명은 WAITLISTED 상태가 된다")
    void concurrentEnrollmentTest() throws InterruptedException {
        // given
        int maxCapacity = 3;
        int threadCount = 10;

        Lecture lecture = Lecture.builder()
                .title("동시성 테스트 강의")
                .description("설명")
                .price(50000)
                .maxCapacity(maxCapacity)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .creatorId(1L)
                .build();
        lecture.changeStatus(LectureStatus.OPEN);
        lectureRepository.save(lecture);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger waitlistCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            long studentId = 10L + i;
            executorService.submit(() -> {
                try {
                    UserInfo student = new UserInfo(studentId, UserInfo.UserRole.STUDENT);
                    EnrollmentCreateRequest request = new EnrollmentCreateRequest(lecture.getId());
                    var response = enrollmentService.enroll(student, request);
                    
                    if (response.getStatus() == EnrollmentStatus.PENDING) {
                        successCount.incrementAndGet();
                    } else if (response.getStatus() == EnrollmentStatus.WAITLISTED) {
                        waitlistCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        assertThat(successCount.get()).isEqualTo(maxCapacity);
        assertThat(waitlistCount.get()).isEqualTo(threadCount - maxCapacity);
        assertThat(failCount.get()).isEqualTo(0);

        int activeCount = enrollmentRepository.countActiveEnrollments(lecture.getId());
        assertThat(activeCount).isEqualTo(maxCapacity);
    }
}
