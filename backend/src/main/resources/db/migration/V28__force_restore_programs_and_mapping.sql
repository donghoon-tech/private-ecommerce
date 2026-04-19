-- V28: 상품 목록 프로그램 완전 초기화 및 강제 연결 
-- 1. 기존에 꼬여있을 수 있는 '상품 목록' 관련 프로그램 권한 및 프로그램 삭제
DELETE FROM role_programs WHERE program_id IN (SELECT id FROM programs WHERE program_code IN ('PG_MKT_LIST', 'API_MKT_SEARCH'));
DELETE FROM programs WHERE program_code IN ('PG_MKT_LIST', 'API_MKT_SEARCH');

-- 2. 프로그램 재생성 (id는 자동 생성)
INSERT INTO programs (category1, category2, program_code, name, url, type, is_public, http_method)
VALUES ('자재마켓', '상품 목록', 'PG_MKT_LIST', '상품 목록 화면', '/', 'WEB', TRUE, 'GET');

INSERT INTO programs (category1, category2, program_code, name, url, type, is_public, http_method)
VALUES ('자재마켓', 'API', 'API_MKT_SEARCH', '상품 검색 API', '/api/products', 'API', TRUE, 'GET');

-- 3. 메뉴 ID(UUID)를 직접 지정하여 프로그램 강제 연결
-- 덤프에서 확인된 '상품목록' 메뉴 ID: a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11
UPDATE menus 
SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_MKT_LIST'),
    path = NULL
WHERE id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11';

-- 4. 모든 주요 역할에 권한 부여
INSERT INTO role_programs (role_id, program_id)
SELECT r.id, p.id FROM roles r, programs p 
WHERE r.name IN ('ADMIN', 'DEVELOPER', 'USER', 'UNVERIFIED') 
  AND p.program_code IN ('PG_MKT_LIST', 'API_MKT_SEARCH');
