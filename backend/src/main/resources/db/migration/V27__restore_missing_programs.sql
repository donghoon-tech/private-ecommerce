-- V27: 사라진 필수 프로그램(상품 목록) 복구 및 메뉴 연결 
-- 1. 상품 목록 화면 (WEB) 프로그램 재생성 (PG_MKT_LIST)
INSERT INTO programs (id, category1, category2, program_code, name, url, type, is_public, http_method)
VALUES (uuid_generate_v4(), '자재마켓', '상품 목록', 'PG_MKT_LIST', '상품 목록 화면', '/', 'WEB', TRUE, 'GET')
ON CONFLICT (program_code) DO UPDATE SET url = '/', is_public = TRUE;

-- 2. 상품 검색 API (API) 프로그램 재생성 (API_MKT_SEARCH)
INSERT INTO programs (id, category1, category2, program_code, name, url, type, is_public, http_method)
VALUES (uuid_generate_v4(), '자재마켓', 'API', 'API_MKT_SEARCH', '상품 검색 API', '/api/products', 'API', TRUE, 'GET')
ON CONFLICT (program_code) DO UPDATE SET url = '/api/products', is_public = TRUE;

-- 3. 메뉴와 프로그램 재연결
UPDATE menus 
SET program_id = (SELECT id FROM programs WHERE program_code = 'PG_MKT_LIST'),
    path = NULL
WHERE name = '상품목록';

-- 4. 권한 부여 (DEVELOPER, ADMIN, USER)
-- ADMIN, DEVELOPER는 모든 관리 권한 포함
INSERT INTO role_programs (role_id, program_id)
SELECT r.id, p.id FROM roles r, programs p 
WHERE r.name IN ('ADMIN', 'DEVELOPER') AND p.program_code IN ('PG_MKT_LIST', 'API_MKT_SEARCH')
ON CONFLICT DO NOTHING;

-- USER 계정도 조회가 기본적으로 가능해야 하므로 추가
INSERT INTO role_programs (role_id, program_id)
SELECT r.id, p.id FROM roles r, programs p 
WHERE r.name = 'USER' AND p.program_code IN ('PG_MKT_LIST', 'API_MKT_SEARCH')
ON CONFLICT DO NOTHING;

-- UNVERIFIED 계정도 조회가 기본적으로 가능해야 하므로 추가
INSERT INTO role_programs (role_id, program_id)
SELECT r.id, p.id FROM roles r, programs p 
WHERE r.name = 'UNVERIFIED' AND p.program_code IN ('PG_MKT_LIST', 'API_MKT_SEARCH')
ON CONFLICT DO NOTHING;
