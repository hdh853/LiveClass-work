-- 초기 테스트 데이터
-- 강의 데이터 (크리에이터 ID: 1, 2)
INSERT INTO lecture (title, description, price, max_capacity, start_date, end_date, status, creator_id, created_at, updated_at)
VALUES ('Spring Boot 마스터 클래스', 'Spring Boot 3.x를 활용한 백엔드 개발 심화 과정', 99000, 3, '2026-06-01', '2026-06-30', 'OPEN', 1, NOW(), NOW());

INSERT INTO lecture (title, description, price, max_capacity, start_date, end_date, status, creator_id, created_at, updated_at)
VALUES ('JPA 실전 활용', 'JPA와 Hibernate를 활용한 데이터 액세스 계층 설계', 79000, 5, '2026-07-01', '2026-07-31', 'OPEN', 1, NOW(), NOW());

INSERT INTO lecture (title, description, price, max_capacity, start_date, end_date, status, creator_id, created_at, updated_at)
VALUES ('Docker & Kubernetes 입문', '컨테이너 기반 배포 환경 구축', 120000, 10, '2026-08-01', '2026-08-31', 'DRAFT', 2, NOW(), NOW());

INSERT INTO lecture (title, description, price, max_capacity, start_date, end_date, status, creator_id, created_at, updated_at)
VALUES ('알고리즘 문제풀이 캠프', '코딩 테스트 대비 알고리즘 집중 과정', 59000, 2, '2026-06-15', '2026-07-15', 'CLOSED', 2, NOW(), NOW());
