# API 명세서

## 인증 방식

모든 API 요청에 아래 HTTP 헤더를 포함합니다:

| 헤더 | 값 | 설명 |
|------|-----|------|
| `X-User-Id` | 숫자 | 사용자 고유 ID |
| `X-User-Role` | `CREATOR` 또는 `STUDENT` | 사용자 역할 |

---

## 강의 (Lecture) API

### 1. 강의 등록

- **URL**: `POST /api/lectures`
- **권한**: CREATOR
- **Request Body**:

```json
{
  "title": "Spring Boot 마스터 클래스",
  "description": "Spring Boot 3.x를 활용한 백엔드 개발 심화 과정",
  "price": 99000,
  "maxCapacity": 30,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30"
}
```

- **Response** (201 Created):

```json
{
  "id": 5,
  "title": "Spring Boot 마스터 클래스",
  "description": "Spring Boot 3.x를 활용한 백엔드 개발 심화 과정",
  "price": 99000,
  "maxCapacity": 30,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "status": "DRAFT",
  "creatorId": 1,
  "createdAt": "2026-05-02T17:00:00.000000"
}
```

- **Validation**:
  - `title`: 필수, 200자 이내
  - `price`: 필수, 0 이상
  - `maxCapacity`: 필수, 1 이상
  - `startDate`: 필수, 오늘 이후
  - `endDate`: 필수, 미래, 시작일 이후

---

### 2. 강의 목록 조회

- **URL**: `GET /api/lectures?status={status}&page={page}&size={size}`
- **권한**: ALL
- **Query Parameters**:

| 파라미터 | 필수 | 기본값 | 설명 |
|---------|------|--------|------|
| `status` | X | 전체 | DRAFT, OPEN, CLOSED |
| `page` | X | 0 | 페이지 번호 (0부터) |
| `size` | X | 10 | 페이지 크기 |

- **Response** (200 OK):

```json
{
  "content": [
    {
      "id": 1,
      "title": "Spring Boot 마스터 클래스",
      "description": "...",
      "price": 99000,
      "maxCapacity": 3,
      "startDate": "2026-06-01",
      "endDate": "2026-06-30",
      "status": "OPEN",
      "creatorId": 1,
      "createdAt": "2026-05-02T17:00:00.000000"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### 3. 강의 상세 조회

- **URL**: `GET /api/lectures/{id}`
- **권한**: ALL
- **Response** (200 OK):

```json
{
  "id": 1,
  "title": "Spring Boot 마스터 클래스",
  "description": "Spring Boot 3.x를 활용한 백엔드 개발 심화 과정",
  "price": 99000,
  "maxCapacity": 3,
  "currentEnrollmentCount": 2,
  "remainingCapacity": 1,
  "startDate": "2026-06-01",
  "endDate": "2026-06-30",
  "status": "OPEN",
  "creatorId": 1,
  "createdAt": "2026-05-02T17:00:00.000000"
}
```

> `currentEnrollmentCount`: 현재 PENDING + CONFIRMED 인원  
> `remainingCapacity`: 남은 정원 (maxCapacity - currentEnrollmentCount)

---

### 4. 강의 상태 변경

- **URL**: `PATCH /api/lectures/{id}/status`
- **권한**: CREATOR (본인 강의만)
- **Request Body**:

```json
{
  "status": "OPEN"
}
```

- **상태 전환 규칙**: `DRAFT` → `OPEN` → `CLOSED` (단방향)
- **Response** (200 OK): LectureResponse 동일
- **Error**: 잘못된 전환 시 `L003 잘못된 상태 전환입니다`

---

### 5. 강의별 수강생 목록 조회

- **URL**: `GET /api/lectures/{id}/students?page={page}&size={size}`
- **권한**: CREATOR (본인 강의만)
- **Response** (200 OK): PENDING + CONFIRMED 상태의 수강 신청 목록 (페이지네이션)

---

## 수강 신청 (Enrollment) API

### 6. 수강 신청

- **URL**: `POST /api/enrollments`
- **권한**: STUDENT
- **Request Body**:

```json
{
  "lectureId": 1
}
```

- **Response (정원 여유 시)** (201 Created):

```json
{
  "id": 1,
  "lectureId": 1,
  "lectureTitle": "Spring Boot 마스터 클래스",
  "studentId": 10,
  "status": "PENDING",
  "waitlistOrder": null,
  "enrolledAt": "2026-05-02T17:10:00.000000",
  "confirmedAt": null,
  "cancelledAt": null,
  "createdAt": "2026-05-02T17:10:00.000000"
}
```

- **Response (정원 초과 시)** (201 Created):

```json
{
  "id": 4,
  "lectureId": 1,
  "lectureTitle": "Spring Boot 마스터 클래스",
  "studentId": 13,
  "status": "WAITLISTED",
  "waitlistOrder": 1,
  "enrolledAt": "2026-05-02T17:15:00.000000",
  "confirmedAt": null,
  "cancelledAt": null,
  "createdAt": "2026-05-02T17:15:00.000000"
}
```

- **비즈니스 규칙**:
  - 강의 상태가 OPEN일 때만 신청 가능
  - 동일 강의 중복 신청 불가 (CANCELLED 제외)
  - 정원 초과 시 자동으로 WAITLISTED 등록

---

### 7. 내 수강 신청 목록 조회

- **URL**: `GET /api/enrollments/me?page={page}&size={size}`
- **권한**: STUDENT
- **Response** (200 OK): 나의 전체 수강 신청 내역 (페이지네이션)

---

### 8. 결제 확정

- **URL**: `PATCH /api/enrollments/{id}/confirm`
- **권한**: STUDENT (본인 신청만)
- **Response** (200 OK):

```json
{
  "id": 1,
  "lectureId": 1,
  "lectureTitle": "Spring Boot 마스터 클래스",
  "studentId": 10,
  "status": "CONFIRMED",
  "waitlistOrder": null,
  "enrolledAt": "2026-05-02T17:10:00.000000",
  "confirmedAt": "2026-05-02T17:20:00.000000",
  "cancelledAt": null,
  "createdAt": "2026-05-02T17:10:00.000000"
}
```

- **비즈니스 규칙**: PENDING 상태에서만 확정 가능

---

### 9. 수강 취소

- **URL**: `PATCH /api/enrollments/{id}/cancel`
- **권한**: STUDENT (본인 신청만)
- **Response** (200 OK):

```json
{
  "id": 1,
  "lectureId": 1,
  "lectureTitle": "Spring Boot 마스터 클래스",
  "studentId": 10,
  "status": "CANCELLED",
  "waitlistOrder": null,
  "enrolledAt": "2026-05-02T17:10:00.000000",
  "confirmedAt": "2026-05-02T17:20:00.000000",
  "cancelledAt": "2026-05-02T17:30:00.000000",
  "createdAt": "2026-05-02T17:10:00.000000"
}
```

- **비즈니스 규칙**:
  - PENDING / WAITLISTED → 즉시 취소
  - CONFIRMED → 결제 확정 후 **7일 이내**만 취소 가능
  - 취소 후 대기열 1순위가 자동으로 PENDING 승격

---

## 에러 응답

### 응답 형식

```json
{
  "timestamp": "2026-05-02T17:00:00.000000",
  "code": "E002",
  "message": "이미 신청한 강의입니다.",
  "status": 409
}
```

### 에러 코드 목록

| 코드 | HTTP | 설명 |
|------|------|------|
| `C001` | 400 | 잘못된 입력값 |
| `C002` | 403 | 접근 권한 없음 |
| `C003` | 401 | 사용자 정보 누락 |
| `L001` | 404 | 강의를 찾을 수 없음 |
| `L002` | 400 | 모집 중이 아닌 강의 |
| `L003` | 400 | 잘못된 상태 전환 |
| `L004` | 403 | 강의 소유자만 가능 |
| `E001` | 404 | 수강 신청 내역 없음 |
| `E002` | 409 | 중복 신청 |
| `E003` | 400 | 잘못된 신청 상태 |
| `E004` | 400 | 취소 기간 만료 |
| `E006` | 403 | 본인 신청만 처리 가능 |
