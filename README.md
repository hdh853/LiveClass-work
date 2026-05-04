# 수강 신청 시스템 (Course Enrollment System)

## 📋 프로젝트 개요

Spring Boot 기반의 수강 신청 REST API 서버입니다.  
크리에이터(강사)가 강의를 개설하고, 클래스메이트(수강생)가 수강 신청 → 결제 확정 → 수강 취소를 수행합니다.

### 주요 기능
- **강의 관리**: 등록, 상태 전환(DRAFT→OPEN→CLOSED), 목록/상세 조회
- **수강 신청**: 신청, 결제 확정, 취소, 내 신청 목록 조회
- **정원 관리**: 최대 정원 초과 시 자동 대기열(Waitlist) 등록
- **대기열 승격**: 취소 발생 시 대기열 1순위 자동 PENDING 승격
- **취소 기간 제한**: 결제 확정 후 7일 이내만 취소 가능
- **동시성 제어**: 비관적 락(Pessimistic Lock)으로 정원 마감 시 race condition 방지

---

## 🛠️ 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 |
| ORM | Spring Data JPA (Hibernate) |
| Database | H2 (In-Memory) |
| Build | Gradle 8.10.2 |
| Test | JUnit 5, Mockito, AssertJ |

---

## 📂 프로젝트 구조

```
src/main/java/com/example/enrollment/
├── EnrollmentApplication.java
├── global/                          # 공통 모듈
│   ├── config/                      # JPA Auditing, WebMvc 설정
│   ├── entity/                      # BaseTimeEntity
│   ├── exception/                   # 에러 코드, 전역 예외 처리
│   └── resolver/                    # 헤더 기반 사용자 인증
├── lecture/                         # 강의 관리
│   ├── controller/
│   ├── domain/
│   ├── dto/
│   ├── repository/
│   └── service/
└── enrollment/                      # 수강 신청
    ├── controller/
    ├── domain/
    ├── dto/
    ├── repository/
    └── service/
```

---

## 📖 문서

| 문서 | 설명 |
|------|------|
| [실행 방법](docs/실행방법.md) | 로컬/Docker 실행 가이드 |
| [API 명세서](docs/API_명세서.md) | 전체 API 엔드포인트 및 요청/응답 예시 |
| [DB 스키마 & ERD](docs/DB_스키마_ERD.md) | 테이블 구조 및 관계도 |

---

## ⚡ 빠른 시작

```bash
# 빌드 및 테스트
./gradlew build

# 서버 실행
./gradlew bootRun

# 서버 실행 후 API 테스트 (포트 8080)
curl -X GET http://localhost:8080/api/lectures?status=OPEN
```

---

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test
```

### 테스트 커버리지
- **LectureServiceTest**: 강의 등록, 상태 전환, 권한 검증, 목록/상세 조회 (6개)
- **EnrollmentServiceTest**: 수강 신청, 정원 초과, 결제 확정, 취소 기간, 대기열 승격, 중복 방지 (11개)

---

## 🏗️ 설계 결정 사항

| 항목 | 결정 | 근거 |
|------|------|------|
| 동시성 제어 | 비관적 락 (`PESSIMISTIC_WRITE`) | 정원 마감 시 데이터 무결성 보장 |
| 대기열 | Enrollment 엔티티의 WAITLISTED 상태 | 별도 테이블 불필요, 관리 단순화 |
| 인증 | HTTP 헤더 기반 (`X-User-Id`, `X-User-Role`) | 과제 요구사항에 따라 간략 처리 |
| 취소 기간 | `application.yml`에서 설정 (기본 7일) | 운영 환경에서 유연하게 변경 가능 |
| 정원 카운트 | PENDING + CONFIRMED 기준 | WAITLISTED는 정원 외 별도 관리 |

---

## 🤖 AI 도구 활용 범위 및 방법

**활용 범위**: 세부 로직 구현, 테스트 코드 뼈대 작성, 더미 데이터 생성 등 단순/반복적인 작업

**활용 방법**
- **설계 주도와 구현 위임**: 수강신청 대기열 및 동시성 제어와 같은 핵심 아키텍처와 인터페이스는 직접 구상 및 설계하고, 세부 코드 구현만 AI에게 위임하여 개발 효율을 극대화했습니다.
- **코드 품질 통제**: AI가 작성한 코드를 그대로 사용하지 않고, 기획 의도와 일치하는지 철저히 리뷰했습니다. 특히 멀티 스레드 환경에서의 예외 상황을 직접 교차 검증하며 최종 퀄리티를 검수했습니다.
- **반복 작업 최소화**: 동시성 통합 테스트 스크립트 작성 등의 작업은 AI에게 전담시켜, '시스템 설계'와 '검증'에 집중했습니다.
